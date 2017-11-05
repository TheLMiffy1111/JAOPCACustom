package thelm.jaopca.custom.json;

import java.lang.reflect.Type;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.config.Configuration;
import thelm.jaopca.api.IOreEntry;
import thelm.jaopca.api.ToFloatFunction;
import thelm.jaopca.api.utils.JsonUtils;
import thelm.jaopca.api.utils.Utils;
import thelm.jaopca.custom.library.EnumConfigValue;
import thelm.jaopca.custom.library.EnumNumberFunction;
import thelm.jaopca.custom.library.EnumPredicate;

public class OreFunctionDeserializers  {

	public static Configuration config;
	public static final Type BOOLEAN_TYPE = new TypeToken<Predicate<IOreEntry>>(){}.getType();
	public static final Type DOUBLE_TYPE = new TypeToken<ToDoubleFunction<IOreEntry>>(){}.getType();
	public static final Type FLOAT_TYPE = new TypeToken<ToFloatFunction<IOreEntry>>(){}.getType();
	public static final Type LONG_TYPE = new TypeToken<ToLongFunction<IOreEntry>>(){}.getType();
	public static final Type INT_TYPE = new TypeToken<ToIntFunction<IOreEntry>>(){}.getType();

	public static enum OreToDoubleFunctionDeserializer implements JsonDeserializer<ToDoubleFunction<IOreEntry>> {
		INSTANCE;

		@Override
		public ToDoubleFunction<IOreEntry> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			if(json.isJsonObject()) {
				JsonObject jsonObj = json.getAsJsonObject();
				String name = JsonUtils.getString(jsonObj, "function");
				EnumNumberFunction functionType = EnumNumberFunction.fromName(name);
				if(functionType == null) {
					throw new JsonParseException("Unsupported function type: "+name);
				}

				switch(functionType) {
				case CONSTANT:
					return parseConstantFunction(jsonObj);
				case POLYNOMIAL:
					return parsePolynomialFunction(jsonObj);
				case POWER:
					return parsePolynomialFunction(jsonObj);
				case EXPONENTIAL:
					return parseExponentialFunction(jsonObj);
				case CONFIG:
					return parseOreConfigFunction(jsonObj);
				}

				throw new JsonParseException("Unsupported function type: "+name);
			}
			throw new JsonParseException("Don't know how to turn "+json+" into a Function");
		}

		ToDoubleFunction<IOreEntry> parseConstantFunction(JsonObject json) throws JsonParseException {
			double max = JsonUtils.getDouble(json, "max", Integer.MAX_VALUE);
			double min = JsonUtils.getDouble(json, "min", Integer.MIN_VALUE);
			double value = JsonUtils.getDouble(json, "value");
			return entry->MathHelper.clamp(value, min, max);
		}

		ToDoubleFunction<IOreEntry> parsePolynomialFunction(JsonObject json) throws JsonParseException {
			double max = JsonUtils.getDouble(json, "max", Integer.MAX_VALUE);
			double min = JsonUtils.getDouble(json, "min", Integer.MIN_VALUE);
			JsonArray jsonArray = JsonUtils.getJsonArray(json, "coefficients");
			double[] coefficients = new double[jsonArray.size()];
			for(int i = 0; i < coefficients.length; ++i) {
				coefficients[i] = JsonUtils.getDouble(jsonArray.get(i), "coefficients"+"["+i+"]");
			}
			ToDoubleFunction<IOreEntry> variable = EnumConfigValue.fromName(JsonUtils.getString(json, "variable", "energy"));
			return entry->{
				double val = 0;
				double var = variable.applyAsDouble(entry);
				for(int i = 0; i < coefficients.length; ++i) {
					val += Math.pow(var, i)*coefficients[coefficients.length-i+1];
				}
				return MathHelper.clamp(val, min, max);
			};
		}

		ToDoubleFunction<IOreEntry> parsePowerFunction(JsonObject json) throws JsonParseException {
			double max = JsonUtils.getDouble(json, "max", Integer.MAX_VALUE);
			double min = JsonUtils.getDouble(json, "min", Integer.MIN_VALUE);
			double exponent = JsonUtils.getDouble(json, "exponent");
			double multiplier = JsonUtils.getDouble(json, "multiplier", 1D);
			ToDoubleFunction<IOreEntry> variable = EnumConfigValue.fromName(JsonUtils.getString(json, "variable", "energy"));
			return entry->MathHelper.clamp(Math.pow(variable.applyAsDouble(entry), exponent)*multiplier, min, max);
		}

		ToDoubleFunction<IOreEntry> parseExponentialFunction(JsonObject json) throws JsonParseException {
			double max = JsonUtils.getDouble(json, "max", Integer.MAX_VALUE);
			double min = JsonUtils.getDouble(json, "min", Integer.MIN_VALUE);
			double base = JsonUtils.getDouble(json, "base");
			double multiplier = JsonUtils.getDouble(json, "multiplier", 1D);
			ToDoubleFunction<IOreEntry> variable = EnumConfigValue.fromName(JsonUtils.getString(json, "variable", "energy"));
			return entry->MathHelper.clamp(Math.pow(base, variable.applyAsDouble(entry))*multiplier, min, max);
		}

		ToDoubleFunction<IOreEntry> parseOreConfigFunction(JsonObject json) throws JsonParseException {
			String key = JsonUtils.getString(json, "key");
			double def = JsonUtils.getDouble(json, "default", 1D);
			return entry->{
				String name = Utils.to_under_score(entry.getOreName());
				double val = config.get(name, key, def).setRequiresMcRestart(true).getDouble();
				if(config.hasChanged()) {
					config.save();
				}
				return val;
			};
		}
	}

	public static enum OreToFloatFunctionDeserializer implements JsonDeserializer<ToFloatFunction<IOreEntry>> {
		INSTANCE;

		@Override
		public ToFloatFunction<IOreEntry> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			return entry->(float)OreToDoubleFunctionDeserializer.INSTANCE.deserialize(json, typeOfT, context).applyAsDouble(entry);
		}
	}

	public static enum OreToLongFunctionDeserializer implements JsonDeserializer<ToLongFunction<IOreEntry>> {
		INSTANCE;

		@Override
		public ToLongFunction<IOreEntry> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			return entry->Math.round(OreToDoubleFunctionDeserializer.INSTANCE.deserialize(json, typeOfT, context).applyAsDouble(entry));
		}
	}

	public static enum OreToIntFunctionDeserializer implements JsonDeserializer<ToIntFunction<IOreEntry>> {
		INSTANCE;

		@Override
		public ToIntFunction<IOreEntry> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			return entry->Math.round(OreToFloatFunctionDeserializer.INSTANCE.deserialize(json, typeOfT, context).applyAsFloat(entry));
		}
	}

	public static enum OrePredicateDeserializer implements JsonDeserializer<Predicate<IOreEntry>> {
		INSTANCE;

		@Override
		public Predicate<IOreEntry> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			if(json.isJsonObject()) {
				JsonObject jsonObj = json.getAsJsonObject();
				String name = JsonUtils.getString(jsonObj, "predicate");
				EnumPredicate predicateType = EnumPredicate.fromName(name);
				if(predicateType == null) {
					throw new JsonParseException("Unsupported predicate type: "+name);
				}

				switch(predicateType) {
				case LESS_THAN:
					return parseLessThanPredicate(jsonObj);
				case GREATER_THAN:
					return parseGreaterThanPredicate(jsonObj);
				case LESS_THAN_OR_EQUAL_TO:
					return parseNotGreaterThanPredicate(jsonObj);
				case GREATER_THAN_OR_EQUAL_TO:
					return parseNotLessThanPredicate(jsonObj);
				case TRUE:
					return entry->true;
				case FALSE:
					return entry->false;
				case CONFIG:
					return parseOreConfigPredicate(jsonObj);
				}

				throw new JsonParseException("Unsupported predicate type: "+name);
			}
			throw new JsonParseException("Don't know how to turn "+json+" into a Predicate");
		}

		Predicate<IOreEntry> parseLessThanPredicate(JsonObject json) throws JsonParseException {
			double value = JsonUtils.getDouble(json, "value");
			ToDoubleFunction<IOreEntry> variable = EnumConfigValue.fromName(JsonUtils.getString(json, "variable", "energy"));
			return entry->variable.applyAsDouble(entry)<value;
		}

		Predicate<IOreEntry> parseGreaterThanPredicate(JsonObject json) throws JsonParseException {
			double value = JsonUtils.getDouble(json, "value");
			ToDoubleFunction<IOreEntry> variable = EnumConfigValue.fromName(JsonUtils.getString(json, "variable", "energy"));
			return entry->variable.applyAsDouble(entry)>value;
		}

		Predicate<IOreEntry> parseNotGreaterThanPredicate(JsonObject json) throws JsonParseException {
			double value = JsonUtils.getDouble(json, "value");
			ToDoubleFunction<IOreEntry> variable = EnumConfigValue.fromName(JsonUtils.getString(json, "variable", "energy"));
			return entry->variable.applyAsDouble(entry)<=value;
		}

		Predicate<IOreEntry> parseNotLessThanPredicate(JsonObject json) throws JsonParseException {
			double value = JsonUtils.getDouble(json, "value");
			ToDoubleFunction<IOreEntry> variable = EnumConfigValue.fromName(JsonUtils.getString(json, "variable", "energy"));
			return entry->variable.applyAsDouble(entry)>=value;
		}

		Predicate<IOreEntry> parseOreConfigPredicate(JsonObject json) throws JsonParseException {
			String key = JsonUtils.getString(json, "key");
			boolean def = JsonUtils.getBoolean(json, "default", false);
			return entry->{
				String name = Utils.to_under_score(entry.getOreName());
				boolean val = config.get(name, key, def).setRequiresMcRestart(true).getBoolean();
				if(config.hasChanged()) {
					config.save();
				}
				return val;
			};
		}
	}
}
