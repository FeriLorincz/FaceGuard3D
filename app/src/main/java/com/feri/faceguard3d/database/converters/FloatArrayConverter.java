package com.feri.faceguard3d.database.converters;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class FloatArrayConverter {

    private static final Gson gson = new Gson();

    @TypeConverter
    public static String fromFloatArray(float[] array) {
        if (array == null) {
            return null;
        }
        return gson.toJson(array);
    }

    @TypeConverter
    public static float[] toFloatArray(String value) {
        if (value == null) {
            return null;
        }
        Type type = new TypeToken<float[]>() {}.getType();
        return gson.fromJson(value, type);
    }
}
