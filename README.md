Note: I am not the original developer of this project. The developer gave me these files, and I have simply open-sourced them. Please do not open issues, as they will not be fixed. Fork this repo and create your own version of the plugin.

Curseforge Page: https://www.curseforge.com/hytale/mods/theeconomy


# EconomySystem - Sistema de Economia Completo para Hytale

## 📋 Índice
- [Visão Geral](#visão-geral)
- [Funcionalidades Principais](#funcionalidades-principais)
- [Comandos](#comandos)
- [Sistemas](#sistemas)
- [Configuração](#configuração)
- [API Pública](#api-pública)
- [Sistema de Tradução](#sistema-de-tradução)
- [HUD](#hud)
- [Armazenamento](#armazenamento)
- [Permissões](#permissões)

---

## 🎯 Visão Geral

O **EconomySystem** é um plugin completo de economia para servidores Hytale, oferecendo um sistema robusto de gerenciamento de dinheiro, lojas administrativas, lojas de jogadores, recompensas automáticas e uma HUD personalizada.

**Versão:** 1.0.5-beta

---

## ✨ Funcionalidades Principais

### 💰 Sistema de Economia
- Gerenciamento completo de saldos de jogadores
- Saldo inicial configurável para novos jogadores
- Formatação de moeda personalizável
- Sistema de ranking (Top 500)
- Transferências entre jogadores

### 🏪 Loja Administrativa
- Loja gerenciada por administradores
- Sistema de tabs (até 7 tabs)
- Compra e venda de itens
- Preços de compra e venda configuráveis
- Interface gráfica completa

### 🛒 Loja de Jogadores
- Cada jogador pode criar sua própria loja
- Sistema de abrir/fechar loja
- Renomear loja
- Definir ícone personalizado
- Sistema de tabs (até 7 tabs por loja)
- Controle de estoque
- Lista de lojas abertas

### 🎁 Sistema de Recompensas
- **Recompensas por Minérios**: Recompensas automáticas ao minerar
- **Recompensas por Madeiras**: Recompensas automáticas ao cortar árvores
- **Recompensas por Monstros**: Recompensas automáticas ao matar monstros
  - Sistema de configuração individual por monstro
  - Cada monstro pode ter sua própria recompensa configurada
  - Configurável via `MonsterRewards` em `EconomyConfig`
  - Logs de debug opcionais para identificar monstros
- Valores configuráveis por tipo de item/monstro

### 📊 HUD Personalizada
- Exibição em tempo real de:
  - Nome do jogador
  - Saldo atual
  - Ranking no Top 500
  - Status da loja (Aberta/Fechada)
  - Notificação de ganhos (temporária, 3 segundos)
- Suporte a códigos de cor Minecraft (`&0-f`, `&l`, `&o`, `&r`)
- Atualização automática (máximo 2 vezes por segundo)
- Pode ser habilitada/desabilitada na configuração

---

## 📝 Comandos

### Comando Principal: `/money` (Aliases: `/eco`, `/balance`, `/bal`)

#### Para Jogadores:
- `/money` - Verifica seu próprio saldo
- `/money <nick>` - Verifica o saldo de outro jogador
- `/money pay <nick> <valor>` - Envia dinheiro para outro jogador
- `/money top [1-500]` - Exibe o ranking dos jogadores mais ricos

#### Para Administradores:
- `/money set <nick> <valor>` - Define o saldo de um jogador
- `/money give <nick> <valor>` - Adiciona dinheiro ao saldo de um jogador

### Comando: `/shop` (Apenas se habilitado)

#### Para Administradores:
- `/shop` - Abre a loja administrativa
- `/shop manager` - Abre o gerenciador da loja
- `/shop add <tab> <preço_compra> <preço_venda>` - Adiciona item à loja
- `/shop remove <uniqueid>` - Remove item da loja
- `/shop tab create <nome>` - Cria uma nova tab
- `/shop tab remove <nome>` - Remove uma tab

### Comando: `/myshop` (Apenas se habilitado)

#### Para Jogadores:
- `/myshop` - Abre o gerenciador da sua loja
- `/myshop open` - Abre sua loja para outros jogadores
- `/myshop close` - Fecha sua loja
- `/myshop rename <nome>` - Renomeia sua loja
- `/myshop add <tab> <preço_compra> <preço_venda>` - Adiciona item à sua loja
- `/myshop remove <uniqueid>` - Remove item da sua loja
- `/myshop tab create <nome>` - Cria uma nova tab na sua loja
- `/myshop tab remove <nome>` - Remove uma tab da sua loja

### Comando: `/playershop <nick>` (Apenas se habilitado)
- Abre a loja de outro jogador (se estiver aberta)

### Comando: `/shops` (Apenas se habilitado)
- Lista todas as lojas de jogadores que estão abertas

### Comando: `/iteminfo`
- Exibe informações sobre o item que você está segurando

---

## ⚙️ Sistemas

### 1. BlockBreakRewardSystem
Sistema que monitora a quebra de blocos e concede recompensas automáticas:
- Detecta minérios minerados
- Detecta madeiras cortadas
- Aplica recompensas configuradas
- Pode ser habilitado/desabilitado

### 2. MonsterKillRewardSystem
Sistema que monitora a morte de monstros e concede recompensas:

**Características:**
- Detecta monstros mortos por jogadores
- Sistema de configuração individual por monstro
- Cada monstro pode ter sua própria recompensa configurada via `MonsterRewards` em `EconomyConfig`
- Identifica monstros por ID (case-insensitive)
- Logs de debug opcionais para identificar monstros não configurados
- Pode ser habilitado/desabilitado

**Como Funciona:**
1. Quando um jogador mata um monstro, o sistema identifica o monstro pelo seu ID
2. Busca a recompensa configurada para aquele monstro específico
3. Se encontrada, adiciona o valor ao saldo do jogador
4. Se `EnableDebugLogs` estiver ativado, registra informações sobre o monstro morto

**Configuração:**
- Configure recompensas individuais para cada tipo de monstro no arquivo de configuração
- Use `EnableDebugLogs: true` para ver os IDs dos monstros quando matá-los
- Monstros não configurados não darão recompensa (mas serão logados se debug estiver ativo)

### 3. EconomyHudSystem
Sistema que gerencia a HUD personalizada:
- Cria HUDs individuais por jogador
- Atualiza informações em tempo real
- Detecta mudanças no saldo para exibir notificações
- Respeita configuração de habilitação/desabilitação

---

## 🔧 Configuração

O arquivo de configuração está localizado em: `config/EconomySystem/EconomySystem.json`

### Opções de Configuração:

#### Configurações Gerais:
- `Language`: Idioma do plugin (PT, EN, ES, RU) - Padrão: "EN"
- `InitialBalance`: Saldo inicial para novos jogadores - Padrão: 1000.0
- `CurrencySymbol`: Símbolo da moeda - Padrão: "$"

#### Habilitar/Desabilitar Sistemas:
- `EnableShop`: Habilita/desabilita loja administrativa - Padrão: true
- `EnablePlayerShop`: Habilita/desabilita lojas de jogadores - Padrão: true
- `EnableHud`: Habilita/desabilita HUD - Padrão: true
- `EnableOreRewards`: Habilita/desabilita recompensas por minérios - Padrão: true
- `EnableWoodRewards`: Habilita/desabilita recompensas por madeiras - Padrão: true
- `EnableMonsterRewards`: Habilita/desabilita recompensas por monstros - Padrão: true
- `EnableDebugLogs`: Habilita/desabilita logs de debug - Padrão: false

#### Recompensas:
- `OreRewards`: Array de recompensas por minério
  - Exemplo: `{"OreName": "ore_iron_stone", "Reward": 1.0}`
- `WoodRewards`: Array de recompensas por madeira
  - Exemplo: `{"WoodName": "wood_fir_trunk", "Reward": 0.5}`
- `MonsterRewards`: Array de recompensas por monstro (configuração individual)
  - Cada monstro pode ter sua própria recompensa configurada
  - Sistema de configuração própria via `MonsterRewardEntry` em `EconomyConfig`
  - Exemplo: `{"MonsterId": "skeleton_fighter", "Reward": 1.0}`
  - Exemplo completo:
    ```json
    "MonsterRewards": [
      {"MonsterId": "skeleton_fighter", "Reward": 1.0},
      {"MonsterId": "crawler_void", "Reward": 2.0},
      {"MonsterId": "rabbit", "Reward": 1.0},
      {"MonsterId": "cow", "Reward": 1.0},
      {"MonsterId": "sheep", "Reward": 1.0}
    ]
    ```
  - **Como identificar IDs de monstros**: 
    - Ative `EnableDebugLogs: true` na configuração
    - Mate um monstro e verifique os logs do servidor
    - O sistema mostrará o ID do monstro morto
    - Adicione o monstro ao array `MonsterRewards` com a recompensa desejada
  - **Monstros padrão configurados**: skeleton_fighter, crawler_void, rabbit, cow, sheep, lamb, calf, tetrabird

#### MySQL (Opcional):
- `EnableMySQL`: Habilita armazenamento MySQL - Padrão: false
- `MySQLHost`: Host do MySQL - Padrão: "localhost"
- `MySQLPort`: Porta do MySQL - Padrão: 3306
- `MySQLUser`: Usuário do MySQL - Padrão: "root"
- `MySQLPassword`: Senha do MySQL - Padrão: ""
- `MySQLDatabaseName`: Nome do banco de dados - Padrão: "theeconomy"
- `MySQLTableName`: Nome da tabela de saldos - Padrão: "bank"
- `MySQLAdminShopTableName`: Nome da tabela da loja admin - Padrão: "adminshop"
- `MySQLPlayerShopTableName`: Nome da tabela de lojas de jogadores - Padrão: "playershop"

---

## 🔌 API Pública

O plugin oferece uma API pública para integração com outros plugins:

### Classe: `com.economy.api.EconomyAPI`

#### Métodos Disponíveis:

```java
// Obter instância
EconomyAPI api = EconomyAPI.getInstance();

// Obter saldo
double balance = api.getBalance(playerUUID);
double balance = api.getBalance("PlayerName");

// Definir saldo
api.setBalance(playerUUID, 1000.0);
api.setBalance("PlayerName", 1000.0);

// Adicionar saldo
api.addBalance(playerUUID, 100.0);
api.addBalance("PlayerName", 100.0);

// Remover saldo
boolean success = api.removeBalance(playerUUID, 50.0);
boolean success = api.removeBalance("PlayerName", 50.0);

// Verificar saldo suficiente
boolean hasEnough = api.hasBalance(playerUUID, 200.0);
boolean hasEnough = api.hasBalance("PlayerName", 200.0);

// Abrir loja administrativa
boolean opened = api.openShop(player); // player é um objeto Player do Hytale
// Retorna true se a loja foi aberta com sucesso, false caso contrário
// Verifica automaticamente se a loja está habilitada na configuração

// Obter saldo formatado
String formatted = api.getFormattedBalance(playerUUID);
String formatted = api.getFormattedBalance("PlayerName");

// Obter informações do jogador
String name = api.getPlayerName(playerUUID);
UUID uuid = api.getPlayerUUID("PlayerName");
```

#### Exemplo de Uso Completo:

```java
import com.economy.api.EconomyAPI;
import com.hypixel.hytale.server.core.entity.entities.Player;

// Em um evento ou comando de outro plugin:
public void onPlayerCommand(Player player) {
    EconomyAPI api = EconomyAPI.getInstance();
    
    // Verifica saldo
    double balance = api.getBalance(player.getUuid());
    
    // Adiciona dinheiro
    api.addBalance(player.getUuid(), 100.0);
    
    // Abre a loja administrativa
    if (api.openShop(player)) {
        // Loja aberta com sucesso
    } else {
        // Falha ao abrir loja (pode estar desabilitada ou jogador offline)
    }
}
```

---

## 🔌 Public API (EconomyAPI)

EconomySystem includes a public API located at: `com.economy.api.EconomyAPI`

### Available API Methods

#### Balance Queries
- `getBalance(UUID playerUUID)` - Get player balance by UUID
- `getBalance(String playerName)` - Get player balance by name

#### Balance Management
- `setBalance(UUID playerUUID, double value)` - Set player balance
- `setBalance(String playerName, double value)` - Set player balance by name *(returns boolean)*
- `addBalance(UUID playerUUID, double value)` - Add money to player balance
- `addBalance(String playerName, double value)` - Add money to player balance by name *(returns boolean)*
- `removeBalance(UUID playerUUID, double value)` - Remove money from player balance *(returns boolean)*
- `removeBalance(String playerName, double value)` - Remove money from player balance by name *(returns boolean)*

#### Balance Checks
- `hasBalance(UUID playerUUID, double value)` - Check if player has enough balance
- `hasBalance(String playerName, double value)` - Check if player has enough balance by name

#### Player Info Helpers
- `getPlayerName(UUID playerUUID)` - Get player name by UUID
- `getPlayerUUID(String playerName)` - Get player UUID by name

#### Formatted Balance
- `getFormattedBalance(UUID playerUUID)` - Get formatted balance string (e.g., "$1,000.00")
- `getFormattedBalance(String playerName)` - Get formatted balance string by name

#### Shop Management
- `openShop(Player player)` - Opens the administrative shop for a player *(returns boolean)*
  - Automatically checks if shop is enabled in configuration
  - Returns `true` if shop was opened successfully, `false` otherwise

### API Example

```java
import com.economy.api.EconomyAPI;
import com.hypixel.hytale.server.core.entity.entities.Player;
import java.util.UUID;

// Get API instance
EconomyAPI api = EconomyAPI.getInstance();

// Balance queries
double balance = api.getBalance(playerUUID);
double balanceByName = api.getBalance("PlayerName");

// Balance management
api.setBalance(playerUUID, 1000.0);
api.addBalance(playerUUID, 100.0);
boolean success = api.removeBalance(playerUUID, 50.0);

// Balance checks
boolean hasEnough = api.hasBalance(playerUUID, 200.0);

// Formatted balance
String formatted = api.getFormattedBalance(playerUUID);

// Player info
String name = api.getPlayerName(playerUUID);
UUID uuid = api.getPlayerUUID("PlayerName");

// Open shop (in an event or command)
public void onPlayerCommand(Player player) {
    EconomyAPI api = EconomyAPI.getInstance();
    
    // Check balance
    double balance = api.getBalance(player.getUuid());
    
    // Add money
    api.addBalance(player.getUuid(), 100.0);
    
    // Open administrative shop
    if (api.openShop(player)) {
        // Shop opened successfully
    } else {
        // Failed to open shop (may be disabled or player offline)
    }
}
```

---

## 🌍 Sistema de Tradução

O plugin suporta múltiplos idiomas com sistema de tradução completo:

### Idiomas Suportados:
- **PT** (Português)
- **EN** (Inglês)
- **ES** (Espanhol)
- **RU** (Russo)

### Organização das Traduções:

As traduções estão organizadas em três categorias:

1. **`chat_`** - Mensagens no Chat
   - Mensagens de comandos
   - Mensagens de erro
   - Mensagens de sucesso
   - Notificações

2. **`gui_`** - Textos das Janelas (GUIs)
   - Títulos de janelas
   - Labels de campos
   - Textos de botões
   - Tooltips
   - Mensagens de confirmação

3. **`hud_`** - Textos da HUD
   - Labels da HUD
   - Status da loja
   - Mensagens temporárias

### Arquivos de Tradução:

Localização: `config/EconomySystem/Language_[IDIOMA].json`

Exemplo: `Language_PT.json`, `Language_EN.json`, etc.

### Sistema de Migração Automática:

O plugin detecta automaticamente arquivos de tradução antigos e migra para o novo formato organizado, criando um backup (`.backup`) antes da migração.

### Códigos de Cor:

O plugin suporta códigos de cor estilo Minecraft:
- `&0-f` - Cores (0=preto, 1=azul escuro, ..., f=branco)
- `&l` - Negrito
- `&o` - Itálico
- `&r` - Reset
- `§` - Também suportado (alternativa ao `&`)

---

## 🖥️ HUD

### Informações Exibidas:

1. **Nick**: Nome do jogador
2. **Money**: Saldo atual formatado
3. **Top Rank**: Posição no ranking (Top 500)
4. **Shop**: Status da loja (Aberta/Fechada)
5. **Gain**: Notificação temporária de ganhos (aparece por 3 segundos quando o saldo aumenta)

### Características:

- **Posição**: Canto superior direito da tela
- **Atualização**: Máximo 2 vezes por segundo
- **Cores**: Suporte completo a códigos de cor
- **Transparência**: Fundo semi-transparente
- **Configurável**: Pode ser habilitada/desabilitada

### Cores Padrão da HUD:

- Labels principais: `&l&6` (Negrito Dourado)
- Loja Aberta: `&a` (Verde)
- Loja Fechada: `&c` (Vermelho)
- Ganhos: `&a` (Verde) com negrito no valor

---

## 💾 Armazenamento

### Modo Padrão (JSON):
- **Saldos**: `config/EconomySystem/Balances.json`
- **Loja Admin**: `config/EconomySystem/Shop.json`
- **Lojas de Jogadores**: `config/EconomySystem/PlayerShop.json`

### Modo MySQL:
Quando habilitado, todos os dados são armazenados em tabelas MySQL:
- Tabela de saldos (configurável)
- Tabela da loja admin (configurável)
- Tabela de lojas de jogadores (configurável)

### Características:
- Salvamento automático periódico
- Salvamento no shutdown do servidor
- Suporte a ambos os modos (JSON e MySQL)
- Migração automática entre modos

---

## 🔐 Permissões

### Permissões de Comandos:

- **`economy.money`**: Usar comandos básicos de dinheiro
- **`economy.money.pay`**: Enviar dinheiro para outros jogadores
- **`economy.money.top`**: Ver ranking de jogadores
- **`economy.money.set`**: Definir saldo (Admin)
- **`economy.money.give`**: Adicionar saldo (Admin)
- **`economy.shop`**: Acessar loja administrativa
- **`economy.shop.manager`**: Gerenciar loja administrativa (Admin)
- **`economy.myshop`**: Gerenciar própria loja
- **`economy.playershop`**: Ver lojas de outros jogadores
- **`economy.iteminfo`**: Ver informações de itens

### Integração com LuckPerms:

O plugin registra automaticamente as permissões no LuckPerms para fácil gerenciamento.

---

## 📦 Estrutura do Projeto

```
EconomySystem/
├── src/main/java/com/economy/
│   ├── api/                    # API pública
│   ├── commands/               # Comandos do plugin
│   │   ├── subcommand/         # Subcomandos
│   │   │   ├── admin/          # Comandos de admin
│   │   │   └── myshop/         # Comandos de loja pessoal
│   ├── config/                 # Classes de configuração
│   ├── economy/                # Gerenciadores de economia
│   ├── files/                  # Gerenciamento de arquivos
│   ├── gui/                    # Interfaces gráficas
│   ├── playershop/             # Sistema de lojas de jogadores
│   ├── shop/                   # Sistema de loja administrativa
│   ├── storage/                # Provedores de armazenamento (MySQL)
│   ├── systems/                # Sistemas de eventos
│   └── util/                   # Utilitários
└── src/main/resources/
    ├── Common/UI/Custom/       # Arquivos UI
    │   ├── Hud/                # HUD
    │   └── Pages/              # Páginas de GUI
    └── Language_*.json         # Arquivos de tradução
```

---

## 🎨 Recursos Adicionais

### Sistema de Formatação de Moeda
- Formatação automática com símbolo configurável
- Suporte a diferentes formatos regionais
- Classe: `CurrencyFormatter`

### Sistema de Mensagens
- Suporte a códigos de cor
- Links clicáveis automáticos
- Formatação avançada
- Classe: `MessageFormatter`

### Sistema de Placeholders
- Integração com PlaceholderAPI (se disponível)
- Placeholders customizados
- Classe: `PlaceholderAPI`

### Gerenciamento de Itens
- Cache de informações de itens
- Nomes traduzidos automaticamente
- Classe: `ItemManager`

---

## 📝 Notas Importantes

1. **Saldo Inicial**: Novos jogadores recebem automaticamente o saldo inicial configurado
2. **Ranking**: O ranking considera apenas os Top 500 jogadores
3. **Lojas**: Cada jogador pode ter apenas uma loja, mas com múltiplas tabs
4. **Estoque**: Lojas de jogadores têm controle de estoque por item
5. **HUD**: A HUD é criada automaticamente quando o jogador entra no mundo
6. **Migração**: Arquivos de tradução antigos são migrados automaticamente
7. **Recompensas por Monstros**: Cada monstro precisa ser configurado individualmente no `MonsterRewards`. Use `EnableDebugLogs: true` para identificar os IDs dos monstros quando matá-los

---

## 🔄 Changelog

### Versão 1.0.5-beta
- ✅ Sistema de cores integrado (Minecraft-like)
- ✅ Reorganização de traduções (chat_, gui_, hud_)
- ✅ Sistema de migração automática de traduções
- ✅ Status da loja na HUD
- ✅ Cores padrão na HUD
- ✅ Melhorias no sistema de atualização da HUD

---

## 📞 Suporte

Para suporte, reportar bugs ou sugerir funcionalidades, consulte a documentação do projeto ou entre em contato com os desenvolvedores.

---

**Desenvolvido para Hytale Server**

