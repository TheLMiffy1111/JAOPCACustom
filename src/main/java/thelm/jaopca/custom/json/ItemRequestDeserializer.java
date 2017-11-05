package thelm.jaopca.custom.json;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.EnumSet;
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
import thelm.jaopca.api.EnumOreType;
import thelm.jaopca.api.IItemRequest;
import thelm.jaopca.api.IOreEntry;
import thelm.jaopca.api.IProperties;
import thelm.jaopca.api.ItemEntry;
import thelm.jaopca.api.ItemEntryGroup;
import thelm.jaopca.api.ToFloatFunction;
import thelm.jaopca.api.block.BlockProperties;
import thelm.jaopca.api.fluid.FluidProperties;
import thelm.jaopca.api.item.ItemProperties;
import thelm.jaopca.api.utils.JsonUtils;
import thelm.jaopca.api.utils.Utils;
import thelm.jaopca.custom.library.EnumMapColor;
import thelm.jaopca.custom.library.EnumMaterial;
import thelm.jaopca.custom.library.EnumNumberFunction;
import thelm.jaopca.custom.library.EnumPredicate;
import thelm.jaopca.custom.library.EnumSoundType;

public enum ItemRequestDeserializer implements JsonDeserializer<IItemRequest> {
	INSTANCE;
	
	@Override
	public IItemRequest deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		if(json.isJsonObject()) {
			JsonObject jsonObj = json.getAsJsonObject();
			return parseItemEntry(jsonObj, context);
		}
		else if(json.isJsonArray()) {
			ItemEntryGroup ret = new ItemEntryGroup();
			for(JsonElement jsonEle : json.getAsJsonArray()) {
				if(jsonEle.isJsonObject()) {
					JsonObject jsonObj = jsonEle.getAsJsonObject();
					ret.entryList.add(parseItemEntry(jsonObj, context));
				}
			}
			return ret;
		}
		throw new JsonParseException("Don't know how to turn "+json+" into a Item Request");
	}

	static ItemEntry parseItemEntry(JsonObject json, JsonDeserializationContext context) throws JsonParseException {
		String name = JsonUtils.getString(json, "name");
		String prefix = JsonUtils.getString(json, "prefix", name);
		String typeName = JsonUtils.getString(json, "type");
		EnumEntryType type = EnumEntryType.fromName(typeName);
		if(type == null) {
			throw new JsonParseException("Unsupported entry type: "+typeName);
		}
		EnumSet<EnumOreType> oreTypes = EnumSet.noneOf(EnumOreType.class);
		if(JsonUtils.hasField(json, "ore_types")) {
			for(JsonElement jsonEle : JsonUtils.getJsonArray(json, "ore_types")) {
				oreTypes.add(EnumOreType.fromName(JsonUtils.getString(jsonEle, "type")));
			}
		}
		else {
			oreTypes.add(EnumOreType.INGOT);
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
			IProperties ppt = type.pptDeserializerContexted.apply(jsonObj, context);
			ret.setProperties(ppt);
		}
		ret.skipWhenGrouped(JsonUtils.getBoolean(json, "skip", false));
		ret.oreTypes.addAll(oreTypes);
		return ret;
	}

	static BlockProperties parseBlockPpt(JsonObject json, JsonDeserializationContext context) throws JsonParseException {
		ToFloatFunction<IOreEntry> hardnessFunc = JsonUtils.deserializeClass(json, "hardness", entry->2F, context, OreFunctionDeserializers.FLOAT_TYPE);
		ToFloatFunction<IOreEntry> resisFunc = JsonUtils.deserializeClass(json, "resistance", entry->hardnessFunc.applyAsFloat(entry)*5F, context, OreFunctionDeserializers.FLOAT_TYPE);
		ToIntFunction<IOreEntry> lgtOpacFunc = JsonUtils.deserializeClass(json, "light_opacity", entry->255, context, OreFunctionDeserializers.INT_TYPE);
		ToFloatFunction<IOreEntry> lgtValFunc = JsonUtils.deserializeClass(json, "light_value", entry->0F, context, OreFunctionDeserializers.FLOAT_TYPE);
		ToFloatFunction<IOreEntry> slippyFunc = JsonUtils.deserializeClass(json, "slipperiness", entry->0.6F, context, OreFunctionDeserializers.FLOAT_TYPE);
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
		ToIntFunction<IOreEntry> flammabFunc = JsonUtils.deserializeClass(json, "flammability", entry->0, context, OreFunctionDeserializers.INT_TYPE);
		ToIntFunction<IOreEntry> fireSpdFunc = JsonUtils.deserializeClass(json, "fire_spread_speed", entry->0, context, OreFunctionDeserializers.INT_TYPE);
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

	static ItemProperties parseItemPpt(JsonObject json, JsonDeserializationContext context) throws JsonParseException {
		int maxStkSize = JsonUtils.getInt(json, "max_stack_size", 64);
		boolean full3d = JsonUtils.getBoolean(json, "full3d", false);
		EnumRarity rarity = rarityFromName(JsonUtils.getString(json, "rarity", "common"));
		Predicate<IOreEntry> hasEffect = JsonUtils.deserializeClass(json, "has_effect", entry->entry.getHasEffect(), context, OreFunctionDeserializers.BOOLEAN_TYPE);
		ItemProperties ppt = new ItemProperties().
				setMaxStackSize(maxStkSize).
				setFull3D(full3d).
				setRarity(rarity);
		return ppt;
	}

	static FluidProperties parseFluidPpt(JsonObject json, JsonDeserializationContext context) throws JsonParseException {
		ToIntFunction<IOreEntry> luminosFunc = JsonUtils.deserializeClass(json, "luminosity", entry->0, context, OreFunctionDeserializers.INT_TYPE);
		ToIntFunction<IOreEntry> densityFunc = JsonUtils.deserializeClass(json, "density", entry->1000, context, OreFunctionDeserializers.INT_TYPE);
		ToIntFunction<IOreEntry> tempFunc = JsonUtils.deserializeClass(json, "temperature", entry->300, context, OreFunctionDeserializers.INT_TYPE);
		ToIntFunction<IOreEntry> viscosFunc = JsonUtils.deserializeClass(json, "viscosity", entry->1000, context, OreFunctionDeserializers.INT_TYPE);
		Predicate<IOreEntry> gaseous = JsonUtils.deserializeClass(json, "gaseous", entry->densityFunc.applyAsInt(entry)<0, context, OreFunctionDeserializers.BOOLEAN_TYPE);
		EnumRarity rarity = rarityFromName(JsonUtils.getString(json, "rarity", "common"));
		SoundEvent fillSound = SoundEvent.REGISTRY.getObject(new ResourceLocation(JsonUtils.getString(json, "fill_sound", "item.bucket.fill")));
		SoundEvent emptySound = SoundEvent.REGISTRY.getObject(new ResourceLocation(JsonUtils.getString(json, "empty_sound", "item.bucket.empty")));
		boolean hasBlock = JsonUtils.getBoolean(json, "has_block", true);
		Material material = EnumMaterial.fromName(JsonUtils.getString(json, "material", "water"));
		ToIntFunction<IOreEntry> quantaFunc = JsonUtils.deserializeClass(json, "quanta_per_block", entry->8, context, OreFunctionDeserializers.INT_TYPE);
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
