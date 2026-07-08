package com.umut.sharedInventory.inventory;

/**
 * 工具类型枚举 — 定义私人背包中可切换的工具区域
 * 对应 Screen 中底部工具栏按钮: C=合成, F=熔炉, B=酿造, A=铁砧, S=锻造台
 */
public enum ToolType {
    CRAFTING,  // 合成台
    FURNACE,   // 熔炉
    BREWING,   // 酿造台
    ANVIL,     // 铁砧
    SMITHING;  // 锻造台

    public static final ToolType[] VALUES = values();
}
