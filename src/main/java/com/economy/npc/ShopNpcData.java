package com.economy.npc;

import java.util.UUID;

/**
 * Dados de persistência para um único NPC da loja
 */
public class ShopNpcData {
    public UUID npcId;
    public String worldUuid;
    public double x, y, z;
    public float yaw, pitch;
    public String name; // Nome do NPC (pode conter códigos de cor como §e)
    public int shopId; // ID da loja associada a este NPC (1+ para NPCs, 0 para /shop)
    public String model; // Modelo do NPC (ex: "klops_merchant", "Outlander_Berserker", etc.)

    public ShopNpcData() {
        this.npcId = UUID.randomUUID();
        this.name = "§eKlops Merchant"; // Nome padrão
        this.shopId = 0; // Será atribuído pelo ShopNpcManager
        this.model = "klops_merchant"; // Modelo padrão
    }

    public ShopNpcData(UUID npcId, String worldUuid, double x, double y, double z, float yaw, float pitch, String name, int shopId) {
        this(npcId, worldUuid, x, y, z, yaw, pitch, name, shopId, "klops_merchant");
    }

    public ShopNpcData(UUID npcId, String worldUuid, double x, double y, double z, float yaw, float pitch, String name, int shopId, String model) {
        this.npcId = npcId != null ? npcId : UUID.randomUUID();
        this.worldUuid = worldUuid;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.shopId = shopId;
        // Aceita tanto & quanto § para códigos de cor, converte & para § para salvar
        if (name != null && !name.trim().isEmpty()) {
            this.name = name.replace('&', '§'); // Converte & para § para salvar no JSON
        } else {
            this.name = "§eKlops Merchant";
        }
        // Define o modelo (padrão: klops_merchant)
        if (model != null && !model.trim().isEmpty()) {
            this.model = model.trim().toLowerCase(); // Normaliza para minúsculo
        } else {
            this.model = "klops_merchant";
        }
    }
}

