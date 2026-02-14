package com.economy.util;

import com.economy.Main;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Utilitário para formatar valores monetários usando a sigla da moeda configurada
 */
public class CurrencyFormatter {

    private static final DecimalFormatSymbols US_FORMAT = new DecimalFormatSymbols(Locale.US);
    
    static {
        US_FORMAT.setGroupingSeparator(',');
        US_FORMAT.setDecimalSeparator('.');
    }

    /**
     * Formata um número com separadores americanos/internacionais (1,000.00)
     */
    private static String formatNumber(double amount, boolean withDecimals) {
        DecimalFormat formatter;
        if (withDecimals) {
            formatter = new DecimalFormat("#,##0.00", US_FORMAT);
        } else {
            formatter = new DecimalFormat("#,##0", US_FORMAT);
        }
        return formatter.format(amount);
    }

    /**
     * Formata um valor monetário com a sigla da moeda configurada
     * @param amount Valor a formatar
     * @return String formatada (ex: "$1,000.50" ou "€1,000.50")
     */
    public static String format(double amount) {
        String symbol = Main.CONFIG != null && Main.CONFIG.get() != null 
            ? Main.CONFIG.get().getCurrencySymbol() 
            : "$";
        
        if (amount == (long) amount) {
            return symbol + formatNumber(amount, false);
        } else {
            return symbol + formatNumber(amount, true);
        }
    }

    /**
     * Formata um valor monetário com a sigla da moeda configurada, sem decimais
     * @param amount Valor a formatar
     * @return String formatada (ex: "$1,000" ou "€1,000")
     */
    public static String formatNoDecimals(double amount) {
        String symbol = Main.CONFIG != null && Main.CONFIG.get() != null 
            ? Main.CONFIG.get().getCurrencySymbol() 
            : "$";
        
        return symbol + formatNumber(amount, false);
    }

    /**
     * Formata um valor monetário com a sigla da moeda configurada, sempre com 2 decimais
     * @param amount Valor a formatar
     * @return String formatada (ex: "$1,000.00" ou "€1,000.00")
     */
    public static String formatWithDecimals(double amount) {
        String symbol = Main.CONFIG != null && Main.CONFIG.get() != null 
            ? Main.CONFIG.get().getCurrencySymbol() 
            : "$";
        
        return symbol + formatNumber(amount, true);
    }

    /**
     * Formata apenas o número sem a sigla da moeda (para uso em mensagens)
     * @param amount Valor a formatar
     * @return String formatada (ex: "1,000.50")
     */
    public static String formatNumberOnly(double amount) {
        return formatNumber(amount, true);
    }

    /**
     * Formata apenas o número sem a sigla da moeda, sem decimais
     * @param amount Valor a formatar
     * @return String formatada (ex: "1,000")
     */
    public static String formatNumberOnlyNoDecimals(double amount) {
        return formatNumber(amount, false);
    }

    /**
     * Formata valores grandes de forma resumida para a GUI da loja
     * Formato "kk": 1,000 = 1k, 1,000,000 = 1kk, 1,000,000,000 = 1kkk, 1,000,000,000,000 = 1kkkk
     * Formato "b": 1,000 = 1k, 1,000,000 = 1m, 1,000,000,000 = 1b, 1,000,000,000,000 = 1t
     * @param amount Valor a formatar
     * @return String formatada (ex: "1k", "1kk", "1kkk" ou "1k", "1m", "1b", "1t")
     */
    public static String formatNumberOnlyShort(double amount) {
        String format = Main.CONFIG != null && Main.CONFIG.get() != null 
            ? Main.CONFIG.get().getShortNumberFormat() 
            : "kk";
        
        boolean useInternational = "b".equalsIgnoreCase(format);
        
        if (amount >= 1_000_000_000_000.0) {
            // 1.000.000.000.000 ou mais (1 trilhão)
            double value = amount / 1_000_000_000_000.0;
            if (useInternational) {
                if (value == (long) value) {
                    return String.format("%.0ft", value);
                } else {
                    return String.format("%.1ft", value);
                }
            } else {
                // Formato kk: 1kkkk
                if (value == (long) value) {
                    return String.format("%.0fkkkk", value);
                } else {
                    return String.format("%.1fkkkk", value);
                }
            }
        } else if (amount >= 1_000_000_000.0) {
            // 1.000.000.000 ou mais (1 bilhão)
            double value = amount / 1_000_000_000.0;
            if (useInternational) {
                if (value == (long) value) {
                    return String.format("%.0fb", value);
                } else {
                    return String.format("%.1fb", value);
                }
            } else {
                // Formato kk: 1kkk
                if (value == (long) value) {
                    return String.format("%.0fkkk", value);
                } else {
                    return String.format("%.1fkkk", value);
                }
            }
        } else if (amount >= 1_000_000.0) {
            // 1.000.000 ou mais (1 milhão)
            double value = amount / 1_000_000.0;
            if (useInternational) {
                if (value == (long) value) {
                    return String.format("%.0fm", value);
                } else {
                    return String.format("%.1fm", value);
                }
            } else {
                // Formato kk: 1kk
                if (value == (long) value) {
                    return String.format("%.0fkk", value);
                } else {
                    return String.format("%.1fkk", value);
                }
            }
        } else if (amount >= 1_000.0) {
            // 1.000 ou mais (1 mil)
            double value = amount / 1_000.0;
            if (value == (long) value) {
                return String.format("%.0fk", value);
            } else {
                return String.format("%.1fk", value);
            }
        } else {
            // Menor que 1.000 = formato normal sem decimais
            return formatNumber(amount, false);
        }
    }

    /**
     * Formata cash como inteiro sem símbolo de moeda
     * @param cash Valor de cash (int)
     * @return String formatada (ex: "1,000")
     */
    public static String formatCash(int cash) {
        return formatNumber(cash, false);
    }
}

