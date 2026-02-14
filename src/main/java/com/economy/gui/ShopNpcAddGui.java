package com.economy.gui;

import com.economy.npc.ShopNpcManager;
import com.economy.util.LanguageManager;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class ShopNpcAddGui extends InteractiveCustomUIPage<ShopNpcAddGui.NpcAddGuiData> {

    private static final HytaleLogger logger = HytaleLogger.getLogger().getSubLogger("EconomySystem");
    private final PlayerRef playerRef;
    private final ShopNpcManager npcManager;
    
    private String currentNpcName = "";
    private String currentModel = "klops_merchant"; // Padrão: klops_merchant

    public ShopNpcAddGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, ShopNpcManager npcManager) {
        super(playerRef, lifetime, NpcAddGuiData.CODEC);
        this.playerRef = playerRef;
        this.npcManager = npcManager;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, 
                     @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/EconomySystem_Shop_Npc_Add.ui");
        
        // Define o título
        String titleText = LanguageManager.getTranslation("gui_shop_npc_add_title");
        uiCommandBuilder.set("#TitleLabel.TextSpans", Message.raw(titleText).color(Color.WHITE));
        
        // Define os labels
        uiCommandBuilder.set("#NpcNameLabel.Text", LanguageManager.getTranslation("gui_shop_npc_add_name"));
        uiCommandBuilder.set("#ModelLabel.Text", LanguageManager.getTranslation("gui_shop_npc_add_model"));
        
        // Define os valores padrão
        uiCommandBuilder.set("#NpcNameField.Value", this.currentNpcName);
        uiCommandBuilder.set("#ModelField.Value", this.currentModel);
        
        // Define os textos dos botões
        String confirmText = LanguageManager.getTranslation("gui_shop_button_confirm");
        String cancelText = LanguageManager.getTranslation("gui_shop_button_cancel");
        uiCommandBuilder.set("#ConfirmButton.Text", confirmText);
        uiCommandBuilder.set("#CancelButton.Text", cancelText);
        
        // Configura os botões de ação
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmButton", 
            EventData.of("Action", "confirm"));
        // Captura o evento do CancelButton para fechar a GUI usando close()
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CancelButton", 
            EventData.of("Action", "cancel"));
        
        // Captura mudanças nos campos de texto
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NpcNameField", 
            EventData.of("@NpcNameField", "#NpcNameField.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ModelField", 
            EventData.of("@ModelField", "#ModelField.Value"), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, 
                          @Nonnull NpcAddGuiData data) {
        super.handleDataEvent(ref, store, data);
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        
        // Captura mudanças nos campos de texto
        if (data.npcNameField != null && !"confirm".equals(data.action) && !"cancel".equals(data.action)) {
            this.currentNpcName = data.npcNameField;
        }
        if (data.modelField != null && !"confirm".equals(data.action) && !"cancel".equals(data.action)) {
            this.currentModel = data.modelField.trim();
            // Se o campo estiver vazio, volta para o padrão
            if (this.currentModel.isEmpty()) {
                this.currentModel = "klops_merchant";
            }
        } else if (this.currentModel == null || this.currentModel.trim().isEmpty()) {
            // Garante que sempre tenha um valor padrão
            this.currentModel = "klops_merchant";
        }
        
        // Se não for ação de confirmar ou cancelar, apenas atualiza os valores
        if (data.action == null || (!"confirm".equals(data.action) && !"cancel".equals(data.action))) {
            return;
        }
        
        // Processa cancel - fecha a GUI usando o método close() da API
        if ("cancel".equals(data.action)) {
            close();
            return;
        }
        
        // Processa confirm
        if ("confirm".equals(data.action)) {
            // Salva os dados necessários antes de processar
            World world = store.getExternalData().getWorld();
            if (world != null) {
                // Salva os dados em variáveis finais para usar no próximo tick
                final String npcName = this.currentNpcName != null ? this.currentNpcName.trim() : "";
                final String npcModel = this.currentModel != null && !this.currentModel.trim().isEmpty() 
                    ? this.currentModel.trim() : "klops_merchant";
                
                // Fecha a GUI primeiro usando o método close() da API do Hytale
                close();
                
                // Agenda a criação do NPC para o próximo tick do mundo
                // Isso garante que a GUI seja fechada antes de criar o NPC
                world.execute(() -> {
                    handleConfirmAsync(ref, store, npcName, npcModel);
                });
            } else {
                // Se não conseguir o mundo, cria diretamente usando os dados atuais
                final String npcName = this.currentNpcName != null ? this.currentNpcName.trim() : "";
                final String npcModel = this.currentModel != null && !this.currentModel.trim().isEmpty() 
                    ? this.currentModel.trim() : "klops_merchant";
                
                // Fecha a GUI primeiro
                close();
                
                handleConfirmAsync(ref, store, npcName, npcModel);
            }
            return;
        }
    }
    
    private void handleConfirmAsync(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, 
                                   String npcName, String npcModel) {
        // Este método é chamado após a GUI ser fechada (no próximo tick do mundo)
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        
        // Obtém o PlayerRef novamente (pode ter mudado após fechar a GUI)
        PlayerRef currentPlayerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (currentPlayerRef == null) {
            player.sendMessage(LanguageManager.getMessage("chat_npc_error_get_player_ref", Color.RED));
            return;
        }
        
        // Obtém a transformação do jogador
        com.hypixel.hytale.math.vector.Transform transform = currentPlayerRef.getTransform();
        if (transform == null) {
            player.sendMessage(LanguageManager.getMessage("chat_npc_error_get_position", Color.RED));
            return;
        }
        
        // Tenta obter a rotação atual do jogador
        try {
            com.hypixel.hytale.server.core.modules.entity.component.TransformComponent playerTransformComp = 
                store.getComponent(ref, com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
            if (playerTransformComp != null) {
                var playerRot = playerTransformComp.getRotation();
                if (playerRot != null) {
                    transform = new com.hypixel.hytale.math.vector.Transform(
                        transform.getPosition(),
                        playerRot
                    );
                }
            }
        } catch (Exception e) {
            // Usa a rotação do PlayerRef se falhar
        }
        
        // Obtém o UUID do mundo
        UUID worldUuidObj = currentPlayerRef.getWorldUuid();
        if (worldUuidObj == null) {
            player.sendMessage(LanguageManager.getMessage("chat_npc_error_get_world_uuid", Color.RED));
            return;
        }
        String worldUuid = worldUuidObj.toString();
        
        // Processa o nome do NPC
        String processedNpcName = npcName;
        if (!processedNpcName.isEmpty()) {
            // Remove aspas se existirem
            while (processedNpcName.startsWith("\"") && processedNpcName.endsWith("\"") && processedNpcName.length() > 1) {
                processedNpcName = processedNpcName.substring(1, processedNpcName.length() - 1).trim();
            }
            // Converte & para § (códigos de cor)
            processedNpcName = processedNpcName.replace('&', '§');
        }
        
        // Obtém o mundo
        World world = store.getExternalData().getWorld();
        if (world == null) {
            player.sendMessage(LanguageManager.getMessage("chat_npc_error_get_world", Color.RED));
            return;
        }
        
        // Cria o NPC
        final com.hypixel.hytale.math.vector.Transform finalTransform = transform;
        final String finalNpcName = processedNpcName.isEmpty() ? null : processedNpcName;
        final String finalModel = npcModel;
        world.execute(() -> {
            try {
                UUID npcId = npcManager.addNpc(finalTransform, worldUuid, finalNpcName, finalModel);
                
                // Monta a mensagem de sucesso
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("npcId", npcId.toString().substring(0, 8));
                
                if (finalNpcName != null && !finalNpcName.isEmpty()) {
                    String displayName = finalNpcName.replaceAll("§[0-9a-fA-Fk-oK-OrR]", "").replaceAll("&[0-9a-fA-Fk-oK-OrR]", "");
                    displayName = displayName.replace("§r", "").replace("&r", "").trim();
                    if (!displayName.isEmpty()) {
                        placeholders.put("name", displayName);
                        player.sendMessage(LanguageManager.getMessage("chat_npc_created_with_name", placeholders));
                    } else {
                        player.sendMessage(LanguageManager.getMessage("chat_npc_created", placeholders));
                    }
                } else {
                    player.sendMessage(LanguageManager.getMessage("chat_npc_created", placeholders));
                }
            } catch (Exception e) {
                logger.at(Level.SEVERE).log("Failed to create NPC: %s", e.getMessage());
                e.printStackTrace();
                player.sendMessage(LanguageManager.getMessage("chat_npc_error_create", Color.RED));
            }
        });
    }

    public static final class NpcAddGuiData {
        private String action;
        private String npcNameField;
        private String modelField;
        
        public static final BuilderCodec<NpcAddGuiData> CODEC = BuilderCodec.<NpcAddGuiData>builder(NpcAddGuiData.class, NpcAddGuiData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (data, value, extraInfo) -> data.action = value,
                        (data, extraInfo) -> data.action).add()
                .append(new KeyedCodec<>("@NpcNameField", Codec.STRING),
                        (data, value, extraInfo) -> data.npcNameField = value,
                        (data, extraInfo) -> data.npcNameField != null ? data.npcNameField : "").add()
                .append(new KeyedCodec<>("@ModelField", Codec.STRING),
                        (data, value, extraInfo) -> data.modelField = value,
                        (data, extraInfo) -> data.modelField != null ? data.modelField : "").add()
                .build();
    }
}

