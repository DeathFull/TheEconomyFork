package com.economy;

import com.economy.commands.*;
import com.economy.config.EconomyConfig;
import com.economy.economy.EconomyManager;
import com.economy.hud.HudPreferenceManager;
import com.economy.integration.VaultUnlockedEconomy;
import com.economy.npc.OpenShopNpcInteraction;
import com.economy.npc.ShopNpcManager;
import com.economy.playershop.PlayerShopManager;
import com.economy.shop.ShopManager;
import com.economy.systems.*;
import com.economy.util.ItemManager;
import com.economy.util.LanguageManager;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.common.semver.SemverRange;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.Config;
import net.cfh.vault.VaultUnlockedServicesManager;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.Collections;
import java.util.UUID;
import java.util.logging.Level;

public class Main extends JavaPlugin {

  public static Config<EconomyConfig> CONFIG;
  private static Main INSTANCE;
  private ShopNpcManager shopNpcManager;

  public Main(@NonNullDecl JavaPluginInit init) {
    super(init);
    CONFIG = this.withConfig("EconomySystem", EconomyConfig.CODEC);
    INSTANCE = this;
  }

  public static Main getInstance() {
    return INSTANCE;
  }

  public ShopNpcManager getShopNpcManager() {
    return shopNpcManager;
  }

  @Override
  protected void setup() {
    super.setup();
    CONFIG.save();

    // Inicializa o HudConfigManager (salva em universe/EconomySystem/HudConfig.json)
    com.economy.config.HudConfigManager.getInstance();

    // Inicializa o sistema de tradução
    LanguageManager.initialize();

    // Inicializa o EconomyManager
    EconomyManager.getInstance();

    // Inicializa o ShopManager
    ShopManager.getInstance();

    // Inicializa o PlayerShopManager
    PlayerShopManager.getInstance();

    // Inicializa o HudPreferenceManager
    HudPreferenceManager.getInstance();

    // Inicializa o ShopNpcManager
    shopNpcManager = new ShopNpcManager();

    // Registra a Interaction e RootInteraction para o NPC da loja
    // Seguindo o padrão do NPCDialog
    try {
      // Primeiro, registra a Interaction no codec
      this.getCodecRegistry(Interaction.CODEC)
              .register("open_shop_npc", OpenShopNpcInteraction.class, OpenShopNpcInteraction.CODEC);
      this.getLogger().at(Level.INFO).log("Registered shop NPC Interaction codec: open_shop_npc");

      // Cria e configura a Interaction
      OpenShopNpcInteraction interaction = new OpenShopNpcInteraction();

      // Tenta definir o ID da interaction via reflexão (como no NPCDialog)
      try {
        java.lang.reflect.Field idField = Interaction.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(interaction, "open_shop_npc");

        // Tenta definir o data field também
        try {
          java.lang.reflect.Field dataField = Interaction.class.getDeclaredField("data");
          dataField.setAccessible(true);
          Class<?> assetExtraInfoClass = Class.forName("com.hypixel.hytale.server.core.asset.type.AssetExtraInfo");
          Object data = assetExtraInfoClass.getDeclaredClasses()[0].getConstructor(
                  Class.class, String.class, String.class
          ).newInstance(Interaction.class, "open_shop_npc", null);
          dataField.set(interaction, data);
        } catch (Exception e) {
          // Ignora se não conseguir definir data
        }
      } catch (Exception e) {
        this.getLogger().at(Level.WARNING).log("Failed to set interaction ID via reflection: %s", e.getMessage());
      }

      // Carrega a Interaction no AssetStore
      Interaction.getAssetStore().loadAssets("EconomySystem", Collections.singletonList(interaction));
      this.getLogger().at(Level.INFO).log("Loaded shop NPC Interaction to AssetStore");

      // Cria e registra o RootInteraction
      try {
        Class<?> rootInteractionClass = Class.forName(
                "com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction");
        Object rootInteraction = rootInteractionClass.getConstructor(String.class, String[].class)
                .newInstance("open_shop_npc", new String[]{"open_shop_npc"});

        // Chama build() (necessário)
        java.lang.reflect.Method buildMethod = rootInteractionClass.getMethod("build");
        buildMethod.invoke(rootInteraction);

        // Carrega o RootInteraction no AssetStore usando o método estático
        java.lang.reflect.Method getAssetStoreMethod = rootInteractionClass.getMethod("getAssetStore");
        Object assetStore = getAssetStoreMethod.invoke(null);
        java.lang.reflect.Method loadAssetsMethod = assetStore.getClass()
                .getMethod("loadAssets", String.class, java.util.List.class);
        loadAssetsMethod.invoke(assetStore, "EconomySystem", Collections.singletonList(rootInteraction));

        this.getLogger().at(Level.INFO).log("Registered and loaded shop NPC RootInteraction: open_shop_npc");
      } catch (Exception e) {
        this.getLogger().at(Level.SEVERE).log("Failed to register shop NPC RootInteraction: %s", e.getMessage());
        e.printStackTrace();
      }
    } catch (Exception e) {
      this.getLogger().at(Level.SEVERE).log("Failed to register shop NPC interaction: %s", e.getMessage());
      e.printStackTrace();
    }

    // Os sistemas que dependem do EntityModule serão registrados no método start()

    // Registra o comando principal
    this.getCommandRegistry().registerCommand(new MoneyCommand());

    // Registra o comando de cash
    this.getCommandRegistry().registerCommand(new CashCommand());

    // Registra o comando de informações do item
    this.getCommandRegistry().registerCommand(new ItemInfoCommand());

    // Registra o comando de HUD apenas se a HUD estiver habilitada
    if (CONFIG.get().isEnableHud()) {
      this.getCommandRegistry().registerCommand(new HudCommand());
    }

    // Registra o comando da loja apenas se estiver habilitado
    if (CONFIG.get().isEnableShop()) {
      this.getCommandRegistry().registerCommand(new ShopCommand(shopNpcManager));
    }

    // Registra os comandos de loja de jogadores apenas se estiver habilitado
    if (CONFIG.get().isEnablePlayerShop()) {
      this.getCommandRegistry().registerCommand(new MyShopCommand());
      this.getCommandRegistry().registerCommand(new PlayerShopCommand());
      this.getCommandRegistry().registerCommand(new ShopsCommand());
    }

    // Registra evento para carregar itens quando os assets são carregados
    this.getEventRegistry().register(LoadedAssetsEvent.class, Item.class, ItemManager::onItemAssetLoad);

    // Registra evento para respawnar os NPCs da loja quando um jogador entrar (mundo já carregado)
    this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, (event) -> {
      if (shopNpcManager != null && shopNpcManager.hasSavedNpcs()) {
        shopNpcManager.spawnFromSaved();
      }
    });

    // Registra evento quando o jogador conecta (antes de entrar no mundo)
    this.getEventRegistry().registerGlobal(PlayerConnectEvent.class, (event) -> {
      try {
        PlayerRef playerRef = event.getPlayerRef();
        Player player = event.getPlayer();
        if (player != null && playerRef != null) {
          UUID playerUuid = playerRef.getUuid();
          EconomyManager.getInstance().setPlayerName(playerUuid, player.getDisplayName());

          // Verifica se é um jogador novo e dá saldo inicial configurável
          if (!EconomyManager.getInstance().hasPlayerBalance(playerUuid)) {
            double initialBalance = CONFIG.get().getInitialBalance();
            EconomyManager.getInstance().setBalance(playerUuid, initialBalance);
          }
        }
      } catch (Exception e) {
        this.getLogger().at(Level.WARNING).log("Error in PlayerConnectEvent: " + e.getMessage());
      }
    });

    // Registra evento quando o jogador entra no mundo
    this.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, (event) -> {
      var holder = event.getHolder();
      var player = holder.getComponent(Player.getComponentType());
      var playerRef = holder.getComponent(PlayerRef.getComponentType());
      if (player != null && playerRef != null) {
        UUID playerUuid = playerRef.getUuid();
        EconomyManager.getInstance().setPlayerName(playerUuid, player.getDisplayName());

        // Verifica se é um jogador novo e dá saldo inicial configurável
        if (!EconomyManager.getInstance().hasPlayerBalance(playerUuid)) {
          double initialBalance = CONFIG.get().getInitialBalance();
          EconomyManager.getInstance().setBalance(playerUuid, initialBalance);
          // O nick já foi atualizado pelo setPlayerName acima
          // Log removed - initial balance is given silently
        }

        // Força a criação da HUD quando o jogador entra no mundo
        // Se o MultipleHUD estiver disponível, adiciona um pequeno delay para garantir
        // que a HUD seja registrada após outros plugins (como SimpleParty) que podem usar o método padrão
        try {
          if (com.economy.util.HudHelper.isMultipleHudAvailable()) {
            // Delay de 1 tick (50ms) para garantir que outros plugins registrem primeiro
            HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
              try {
                com.economy.systems.EconomyHudSystem.createHudForPlayer(player, playerRef);
              } catch (Exception e) {
                this.getLogger().at(Level.WARNING).log("Failed to create HUD after delay: " + e.getMessage());
              }
            }, 50, java.util.concurrent.TimeUnit.MILLISECONDS);
          } else {
            // Sem delay se não estiver usando MultipleHUD
            com.economy.systems.EconomyHudSystem.createHudForPlayer(player, playerRef);
          }
        } catch (Exception e) {
          this.getLogger().at(Level.WARNING).log("Failed to create HUD in AddPlayerToWorldEvent: " + e.getMessage());
        }
      }
    });


    // Verifica se VaultUnlocked está instalado (opcional - apenas se o JAR estiver presente)
    checkVaultUnlocked();

    // Verifica se Cassaforte está instalado e inicializa o serviço de economia (opcional)
    boolean cassaforteAvailable = checkCassaforte();

    // Resumo das configurações de recompensas
    StringBuilder rewardsSummary = new StringBuilder("Rewards: ");
    if (CONFIG.get().isEnableOreRewards()) {
      rewardsSummary.append("Ores(").append(CONFIG.get().getOreRewards().size()).append(") ");
    }
    if (CONFIG.get().isEnableWoodRewards()) {
      rewardsSummary.append("Woods(").append(CONFIG.get().getWoodRewards().size()).append(") ");
    }
    if (CONFIG.get().isEnableMonsterRewards()) {
      rewardsSummary.append("Monsters(").append(CONFIG.get().getMonsterRewards().size()).append(")");
    }
    if (rewardsSummary.length() > 9) {
      this.getLogger().at(Level.INFO).log(rewardsSummary.toString().trim());
    }

    // Log de inicialização com integrações
    if (cassaforteAvailable) {
      if (com.economy.util.CassaforteEconomy.isAvailable()) {
        // Cassaforte está instalado e há um provedor de economia registrado
        this.getLogger().at(Level.INFO).log("EconomySystem loaded successfully with Cassaforte integration");
      } else {
        // Cassaforte está instalado mas nenhum provedor de economia registrou seu serviço
        // EconomySystem continuará usando seu próprio sistema de economia
        // Mas ainda assim consideramos que o Cassaforte está "integrado" (disponível)
        this.getLogger()
                .at(Level.INFO)
                .log("EconomySystem loaded successfully with Cassaforte (no economy provider registered - using own " +
                        "economy system)");
      }
    } else {
      this.getLogger().at(Level.INFO).log("EconomySystem loaded successfully");
    }
  }

  /**
   * Verifica se VaultUnlocked está instalado e registra o provedor de economia.
   * Baseado em: https://tne.gitbook.io/vaultunlocked/hytale/economy-provider
   */
  private void checkVaultUnlocked() {
    if (HytaleServer.get().getPluginManager().hasPlugin(
            PluginIdentifier.fromString("TheNewEconomy:VaultUnlocked"),
            SemverRange.WILDCARD
    )) {
      getLogger().atInfo().log("VaultUnlocked is installed, enabling VaultUnlocked support.");

      VaultUnlockedServicesManager.get().economy(new VaultUnlockedEconomy(this));
    } else {
      getLogger().atInfo().log("VaultUnlocked is not installed, disabling VaultUnlocked support.");
    }
  }

  /**
   * Verifica se Cassaforte está instalado e inicializa o serviço de economia (opcional)
   * Baseado em: https://www.curseforge.com/hytale/mods/cassaforte
   * Plugin identifier: it.cassaforte:Cassaforte
   *
   * @return true se o Cassaforte foi detectado e inicializado com sucesso
   */
  private boolean checkCassaforte() {
    try {
      // Abordagem simplificada: verifica diretamente se a classe do Cassaforte existe
      // Se a classe existir, significa que o plugin está disponível
      Class<?> cassaforteClass = Class.forName("it.cassaforte.api.Cassaforte");

      // Se chegou aqui, a classe existe - inicializa o serviço
      this.getLogger().at(Level.INFO).log("Cassaforte plugin detected, initializing economy service...");
      com.economy.util.CassaforteEconomy.initialize();

      // Verifica se a inicialização foi bem-sucedida
      if (com.economy.util.CassaforteEconomy.isAvailable()) {
        this.getLogger().at(Level.INFO).log("Cassaforte integration enabled");
        return true;
      } else {
        this.getLogger().at(Level.WARNING).log("Cassaforte plugin found but economy service not available");
        return false;
      }

    } catch (ClassNotFoundException e) {
      // Cassaforte classes não estão disponíveis - isso é normal se o JAR não estiver presente
      // Não loga nada, pois é esperado quando o JAR não está presente
      return false;
    } catch (Exception e) {
      // Outros erros são silenciosamente ignorados
      // Cassaforte é opcional, então não queremos causar erros se não estiver disponível
      this.getLogger().at(Level.WARNING).log("Error checking for Cassaforte: %s", e.getMessage());
      return false;
    }
  }

  @Override
  protected void start() {
    super.start();

    // Registra os sistemas que dependem do EntityModule aqui (após todas as dependências estarem disponíveis)
    // Verifica se o EntityModule está disponível antes de tentar registrar os sistemas
    try {
      // Tenta acessar o EntityStoreRegistry - se falhar, significa que EntityModule não está disponível
      var entityStoreRegistry = this.getEntityStoreRegistry();

      if (entityStoreRegistry != null) {
        // Registra o sistema de rastreamento de blocos colocados (deve ser registrado primeiro)
        entityStoreRegistry.registerSystem(new BlockPlaceTrackerSystem());

        // Registra o sistema de recompensas por quebra de blocos
        entityStoreRegistry.registerSystem(new BlockBreakRewardSystem());

        // Registra o sistema de recompensas por matar monstros usando DamageEventSystem
        entityStoreRegistry.registerSystem(new MonsterKillRewardSystem());

        // Registra o sistema de HUD
        entityStoreRegistry.registerSystem(new EconomyHudSystem());

        // Registra o sistema de log de interações com blocos
        entityStoreRegistry.registerSystem(new BlockInteractLogSystem());

        this.getLogger().at(Level.INFO).log("Entity systems registered successfully");
      } else {
        this.getLogger().at(Level.WARNING).log("EntityModule not available - entity systems will not be registered");
      }
    } catch (Exception e) {
      this.getLogger()
              .at(Level.WARNING)
              .log("EntityModule not available - entity systems will not be registered: %s", e.getMessage());
    }

    // Spawna os NPCs salvos após os sistemas estarem registrados
    if (shopNpcManager != null && shopNpcManager.hasSavedNpcs()) {
      shopNpcManager.spawnFromSaved();
    }
  }

  @Override
  protected void shutdown() {
    this.getLogger().at(Level.INFO).log("EconomySystem shutting down - saving data...");
    // Shutdown HudConfigManager (não salva HUD config - apenas lê do JSON)
    com.economy.config.HudConfigManager hudConfigManager = com.economy.config.HudConfigManager.getInstance();
    if (hudConfigManager != null) {
      hudConfigManager.shutdown();
    }
    // Shutdown EconomyManager (saves all data and closes MySQL connection if used)
    EconomyManager.getInstance().shutdown();
    // Shutdown ShopManager (saves all data and closes MySQL connection if used)
    ShopManager.getInstance().shutdown();
    // Shutdown PlayerShopManager (saves all data and closes MySQL connection if used)
    PlayerShopManager.getInstance().shutdown();
    this.getLogger().at(Level.INFO).log("EconomySystem shutdown complete");
  }

}
