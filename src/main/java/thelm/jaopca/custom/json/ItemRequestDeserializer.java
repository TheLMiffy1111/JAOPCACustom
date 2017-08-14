package thelm.jaopca.custom.json;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.EnumRarity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import thelm.jaopca.api.EnumEntryType;
import thelm.jaopca.api.IItemRequest;
import thelm.jaopca.api.IOreEntry;
import thelm.jaopca.api.ItemEntry;
import thelm.jaopca.api.ItemEntryGroup;
import thelm.jaopca.api.ToFloatFunction;
import thelm.jaopca.api.block.BlockProperties;
import thelm.jaopca.api.fluid.FluidProperties;
import thelm.jaopca.api.item.ItemProperties;
import thelm.jaopca.api.utils.Utils;
import thelm.jaopca.custom.library.EnumMapColor;
import thelm.jaopca.custom.library.EnumMaterial;
import thelm.jaopca.custom.library.EnumNumberFunction;
import thelm.jaopca.custom.library.EnumPredicate;
import thelm.jaopca.custom.library.EnumSoundType;

public class ItemRequestDeserializer implements JsonDeserializer<IItemRequest> {

	@Override
	public IItemRequest deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		if(json.isJsonObject()) {
			JsonObject jsonObj = json.getAsJsonObject();
			return parseItemEntry(jsonObj);
		}
		else if(json.isJsonArray()) {
			ItemEntryGroup ret = new ItemEntryGroup();
			for(JsonElement jsonEle : json.getAsJsonArray()) {
				if(json.isJsonObject()) {
					JsonObject jsonObj = jsonEle.getAsJsonObject();
					ret.entryList.add(parseItemEntry(jsonObj));
					return ret;
				}
			}
		}
		throw new JsonParseException("Don\'t know how to turn "+json+" into a Item Request");
	}

	static ItemEntry parseItemEntry(JsonObject json) throws JsonParseException {
		String name = JsonUtils.getString(json, "name");
		String prefix = JsonUtils.getString(json, "prefix", name);
		String typeName = JsonUtils.getString(json, "type");
		EnumEntryType type = entryTypeFromName(typeName);
		if(type == null) {
			throw new JsonParseException("Unsupported entry type: "+name);
		}
		ModelResourceLocation itemModelLocation = new ModelResourceLocation("jaopca:"+(type==EnumEntryType.FLUID?"fluid/":"")+Utils.to_under_score(name)+'#'+
				(type==EnumEntryType.ITEM?"inventory":"normal"));
		ArrayList<String> blacklist = Lists.<String>newArrayList();
		if(JsonUtils.hasField(json, "blacklist")) {
			for(JsonElement jsonEle : JsonUtils.getJsonArray(json, "blacklist")) {
				blacklist.add(JsonUtils.getString(jsonEle, "ore"));
			}
		}
		ItemEntry ret = new ItemEntry(type, name, prefix, itemModelLocation, blacklist);
		if(JsonUtils.hasField(json, "properties")) {
			JsonObject jsonObj = JsonUtils.getJsonObject(json, "properties");
			switch(type) {
			case BLOCK:
				BlockProperties bppt = parseBlockPpt(jsonObj);
				ret.setBlockProperties(bppt);
				break;
			case ITEM:
				ItemProperties ippt = parseItemPpt(jsonObj);
				ret.setItemProperties(ippt);
				break;
			case FLUID:
				FluidProperties fppt = parseFluidPpt(jsonObj);
				ret.setFluidProperties(fppt);
				break;
			case CUSTOM:
				break;
			}
		}
		ret.skipWhenGrouped(JsonUtils.getBoolean(json, "skip", false));
		return ret;
	}

	static BlockProperties parseBlockPpt(JsonObject json) throws JsonParseException {
		ToFloatFunction<IOreEntry> hardnessFunc;
		if(JsonUtils.hasField(json, "hardness")) {
			hardnessFunc = parseFloatFunction(JsonUtils.getJsonObject(json, "hardness"));
		}
		else {
			hardnessFunc = entry->2F;
		}
		ToFloatFunction<IOreEntry> resisFunc;
		if(JsonUtils.hasField(json, "resistance")) {
			resisFunc = parseFloatFunction(JsonUtils.getJsonObject(json, "resistance"));
		}
		else {
			resisFunc = entry->hardnessFunc.applyAsFloat(entry)*5F;
		}
		ToIntFunction<IOreEntry> lgtOpacFunc;
		if(JsonUtils.hasField(json, "light_opacity")) {
			lgtOpacFunc = parseIntFunction(JsonUtils.getJsonObject(json, "light_opacity"));
		}
		else {
			lgtOpacFunc = entry->255;
		}
		ToFloatFunction<IOreEntry> lgtValFunc;
		if(JsonUtils.hasField(json, "light_value")) {
			lgtValFunc = parseFloatFunction(JsonUtils.getJsonObject(json, "light_value"));
		}
		else {
			lgtValFunc = entry->0F;
		}
		ToFloatFunction<IOreEntry> slippyFunc;
		if(JsonUtils.hasField(json, "slipperiness")) {
			slippyFunc = parseFloatFunction(JsonUtils.getJsonObject(json, "slipperiness"));
		}
		else {
			slippyFunc = entry->0.6F;
		}
		Material material = EnumMaterial.fromName(JsonUtils.getString(json, "material", "rock"));
		MapColor mapColor = material.getMaterialMapColor();
		if(JsonUtils.isNumber(json, "map_color")) {
			mapColor = MapColor.COLORS[JsonUtils.getInt(json, "map_color")];
		}
		else if(JsonUtils.isString(json, "map_color")) {
			mapColor = EnumMapColor.fromName(JsonUtils.getString(json, "map_color"));
		}
		SoundType soundType = EnumSoundType.fromName(JsonUtils.getString(json, "sound_type", "stone"));
		int maxStkSize = JsonUtils.getInt(json, "max_stack_size", 64);
		EnumRarity rarity = rarityFromName(JsonUtils.getString(json, "rarity", "common"));
		boolean fallable = JsonUtils.getBoolean(json, "fallable", false);
		boolean beaconBase = JsonUtils.getBoolean(json, "beacon_base", false);
		AxisAlignedBB boundingBox = parseBoundingBox(json, "bounding_box", Block.FULL_BLOCK_AABB);
		String harvestTool = JsonUtils.getString(json, "harvest_tool", null);
		int harvestLevel = JsonUtils.getInt(json, "harvest_level", -1);
		boolean full = JsonUtils.getBoolean(json, "full", true);
		boolean opaque = JsonUtils.getBoolean(json, "opaque", true);
		BlockRenderLayer renderLayer = layerFromName(JsonUtils.getString(json, "render_layer", "cutout"));
		ToIntFunction<IOreEntry> flammabFunc;
		if(JsonUtils.hasField(json, "flammability")) {
			flammabFunc = parseIntFunction(JsonUtils.getJsonObject(json, "flammability"));
		}
		else {
			flammabFunc = entry->0;
		}
		ToIntFunction<IOreEntry> fireSpdFunc;
		if(JsonUtils.hasField(json, "fire_spread_speed")) {
			fireSpdFunc = parseIntFunction(JsonUtils.getJsonObject(json, "fire_spread_speed"));
		}
		else {
			fireSpdFunc = entry->0;
		}
		boolean fireSource = JsonUtils.getBoolean(json, "fire_source", false);
		BlockProperties ppt = new BlockProperties().
				setHardnessFunc(hardnessFunc).
				setResistanceFunc(resisFunc).
				setLightOpacityFunc(lgtOpacFunc).
				setLightValueFunc(lgtValFunc).
				setSlipperinessFunc(slippyFunc).
				setMaterial(material).
				setMapColor(mapColor).
				setSoundType(soundType).
				setMaxStackSize(maxStkSize).
				setRarity(rarity).
				setFallable(fallable).
				setBeaconBase(beaconBase).
				setBoundingBox(boundingBox).
				setHarvestTool(harvestTool).
				setHarvestLevel(harvestLevel).
				setFull(full).
				setOpaque(opaque).
				setBlockLayer(renderLayer).
				setFlammabilityFunc(flammabFunc).
				setFireSpreadSpeedFunc(fireSpdFunc).
				setFireSource(fireSource);
		return ppt;
	}

	static ItemProperties parseItemPpt(JsonObject json) throws JsonParseException {
		int maxStkSize = JsonUtils.getInt(json, "max_stack_size", 64);
		boolean full3d = JsonUtils.getBoolean(json, "full3d", false);
		EnumRarity rarity = rarityFromName(JsonUtils.getString(json, "rarity", "common"));
		ItemProperties ppt = new ItemProperties().
				setMaxStackSize(maxStkSize).
				setFull3D(full3d).
				setRarity(rarity);
		return ppt;
	}

	static FluidProperties parseFluidPpt(JsonObject json) throws JsonParseException {
		ToIntFunction<IOreEntry> luminosFunc;
		if(JsonUtils.hasField(json, "luminosity")) {
			luminosFunc = parseIntFunction(JsonUtils.getJsonObject(json, "luminosity"));
		}
		else {
			luminosFunc = entry->0;
		}
		ToIntFunction<IOreEntry> densityFunc;
		if(JsonUtils.hasField(json, "density")) {
			densityFunc = parseIntFunction(JsonUtils.getJsonObject(json, "density"));
		}
		else {
			densityFunc = entry->1000;
		}
		ToIntFunction<IOreEntry> tempFunc;
		if(JsonUtils.hasField(json, "temperature")) {
			tempFunc = parseIntFunction(JsonUtils.getJsonObject(json, "temperature"));
		}
		else {
			tempFunc = entry->300;
		}
		ToIntFunction<IOreEntry> viscosFunc;
		if(JsonUtils.hasField(json, "viscosity")) {
			viscosFunc = parseIntFunction(JsonUtils.getJsonObject(json, "viscosity"));
		}
		else {
			viscosFunc = entry->1000;
		}
		Predicate<IOreEntry> gaseous;
		if(JsonUtils.hasField(json, "gaseous")) {
			gaseous = parsePredicate(JsonUtils.getJsonObject(json, "viscosity"));
		}
		else {
			gaseous = entry->densityFunc.applyAsInt(entry)<0;
		}
		EnumRarity rarity = rarityFromName(JsonUtils.getString(json, "rarity", "common"));
		SoundEvent fillSound = SoundEvent.REGISTRY.getObject(new ResourceLocation(JsonUtils.getString(json, "fill_sound", "item.bucket.fill")));
		SoundEvent emptySound = SoundEvent.REGISTRY.getObject(new ResourceLocation(JsonUtils.getString(json, "empty_sound", "item.bucket.empty")));
		boolean hasBlock = JsonUtils.getBoolean(json, "has_block", true);
		Material material = EnumMaterial.fromName(JsonUtils.getString(json, "material", "water"));
		ToIntFunction<IOreEntry> quantaFunc;
		if(JsonUtils.hasField(json, "quanta_per_block")) {
			quantaFunc = parseIntFunction(JsonUtils.getJsonObject(json, "quanta_per_block"));
		}
		else {
			quantaFunc = entry->8;
		}
		FluidProperties ppt = new FluidProperties().
				setLuminosityFunc(luminosFunc).
				setDensityFunc(densityFunc).
				setTemperatureFunc(tempFunc).
				setViscosityFunc(viscosFunc).
				setGaseousPredicate(gaseous).
				setRarity(rarity).
				setFillSound(fillSound).
				setEmptySound(emptySound).
				setHasBlock(hasBlock).
				setMaterial(material).
				setQuantaFunc(quantaFunc);
		return ppt;
	}

	static AxisAlignedBB parseBoundingBox(JsonObject object, String memberName, AxisAlignedBB fallback) throws JsonParseException {
		if(JsonUtils.hasField(object, memberName)) {
			JsonArray jsonarray = JsonUtils.getJsonArray(object, memberName);

			if(jsonarray.size() != 6) {
				throw new JsonParseException("Expected 6 "+memberName+" values, found: "+jsonarray.size());
			}

			double[] adouble = new double[6];

			for(int i = 0; i < adouble.length; ++i) {
				adouble[i] = JsonUtils.getDouble(jsonarray.get(i), memberName+"["+i+"]");
			}

			return new AxisAlignedBB(adouble[0], adouble[1], adouble[2], adouble[3], adouble[4], adouble[5]);
		}
		return fallback;
	}

	static ToIntFunction<IOreEntry> parseIntFunction(JsonObject json) throws JsonParseException {
		return entry->(int)parseDoubleFunction(json).applyAsDouble(entry);
	}

	static ToFloatFunction<IOreEntry> parseFloatFunction(JsonObject json) {
		return entry->(float)parseDoubleFunction(json).applyAsDouble(entry);
	}

	static ToDoubleFunction<IOreEntry> parseDoubleFunction(JsonObject json) throws JsonParseException {
		String name = JsonUtils.getString(json, "function");
		EnumNumberFunction functionType = EnumNumberFunction.fromName(name);
		if(functionType == null) {
			throw new JsonParseException("Unsupported function type: "+name);
		}

		double max = JsonUtils.getDouble(json, "max", Integer.MAX_VALUE);
		double min = JsonUtils.getDouble(json, "min", Integer.MIN_VALUE);

		switch(functionType) {
		case CONSTANT:
			return entry->MathHelper.clamp(JsonUtils.getDouble(json, "value"), min, max);
		case POLYNOMIAL:
			return entry->MathHelper.clamp(parsePolynomialFunction(json).applyAsDouble(entry), min, max);
		case POWER:
			return entry->MathHelper.clamp(Math.pow(entry.getEnergyModifier(), JsonUtils.getDouble(json, "exponent"))*JsonUtils.getDouble(json, "multiplier", 1D), min, max);
		case EXPONENTIAL:
			return entry->MathHelper.clamp(Math.pow(JsonUtils.getDouble(json, "base"), entry.getEnergyModifier())*JsonUtils.getDouble(json, "multiplier", 1D), min, max);
		}

		throw new JsonParseException("Unsupported function type: "+name);
	}

	static ToDoubleFunction<IOreEntry> parsePolynomialFunction(JsonObject json) throws JsonParseException {
		JsonArray jsonArray = JsonUtils.getJsonArray(json, "coefficients");

		double[] coefficients = new double[jsonArray.size()];

		for(int i = 0; i < coefficients.length; ++i) {
			coefficients[i] = JsonUtils.getDouble(jsonArray.get(i), "coefficients"+"["+i+"]");
		}

		return (entry)->{
			double val = 0;
			for(int i = 0; i < coefficients.length; --i) {
				val += Math.pow(entry.getEnergyModifier(), i)*coefficients[coefficients.length-i+1];
			}
			return val;
		};
	}

	static Predicate<IOreEntry> parsePredicate(JsonObject json) {
		String name = JsonUtils.getString(json, "predicate");
		EnumPredicate predicateType = EnumPredicate.fromName(name);
		if(predicateType == null) {
			throw new JsonParseException("Unsupported predicate type: "+name);
		}

		switch(predicateType) {
		case LESS_THAN:
			return entry->entry.getEnergyModifier()<JsonUtils.getDouble(json, "value");
		case GREATER_THAN:
			return entry->entry.getEnergyModifier()>JsonUtils.getDouble(json, "value");
		case LESS_THAN_OR_EQUAL_TO:
			return entry->entry.getEnergyModifier()<=JsonUtils.getDouble(json, "value");
		case GREATER_THAN_OR_EQUAL_TO:
			return entry->entry.getEnergyModifier()>=JsonUtils.getDouble(json, "value");
		case TRUE:
			return entry->true;
		case FALSE:
			return entry->false;
		}

		throw new JsonParseException("Unsupported predicate type: "+name);
	}

	static EnumEntryType entryTypeFromName(String name) {
		for(EnumEntryType type : EnumEntryType.values()) {
			if(type.name().equalsIgnoreCase(name)) {
				return type;
			}
		}
		return null;
	}

	static EnumRarity rarityFromName(String name) {
		for(EnumRarity rarity : EnumRarity.values()) {
			if(rarity.name().equalsIgnoreCase(name)) {
				return rarity;
			}
		}
		return EnumRarity.COMMON;
	}

	static BlockRenderLayer layerFromName(String name) {
		for(BlockRenderLayer layer : BlockRenderLayer.values()) {
			if(layer.name().equalsIgnoreCase(name)) {
				return layer;
			}
		}
		return BlockRenderLayer.CUTOUT;
	}
}
