package com.economy.npc;

import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper para a lista de NPCs da loja
 */
public class ShopNpcList {
    public List<ShopNpcData> npcs;

    public ShopNpcList() {
        this.npcs = new ArrayList<>();
    }

    public ShopNpcList(List<ShopNpcData> npcs) {
        this.npcs = npcs != null ? npcs : new ArrayList<>();
    }

    public static TypeToken<ShopNpcList> getTypeToken() {
        return new TypeToken<ShopNpcList>() {};
    }
}

