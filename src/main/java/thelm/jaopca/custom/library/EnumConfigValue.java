package thelm.jaopca.custom.library;

import java.util.function.ToDoubleFunction;

import thelm.jaopca.api.IOreEntry;

public enum EnumConfigValue {
	ENERGY(entry->entry.getEnergyModifier()),
	RARITY(entry->entry.getRarity());

	public final ToDoubleFunction<IOreEntry> entryToValue;

	EnumConfigValue(ToDoubleFunction<IOreEntry> func) {
		entryToValue = func;
	}

	public static ToDoubleFunction<IOreEntry> fromName(String name) {
		for(EnumConfigValue configValue : EnumConfigValue.values()) {
			if(configValue.name().equalsIgnoreCase(name)) {
				return configValue.entryToValue;
			}
		}
		return entry->1D;
	}
}
