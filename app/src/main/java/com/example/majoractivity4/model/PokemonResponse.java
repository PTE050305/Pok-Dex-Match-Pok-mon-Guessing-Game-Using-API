package com.example.majoractivity4.model;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PokemonResponse {

    private int id;
    private String name;
    private Sprites sprites;
    private List<TypeSlot> types;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPrimaryType() {
        if (types == null || types.isEmpty()) {
            return null;
        }

        Collections.sort(types, Comparator.comparingInt(TypeSlot::getSlot));
        return types.get(0).getTypeName();
    }

    public String getSecondaryType() {
        if (types == null || types.size() < 2) {
            return null;
        }

        Collections.sort(types, Comparator.comparingInt(TypeSlot::getSlot));
        return types.get(1).getTypeName();
    }

    public String getBestImageUrl() {
        if (sprites == null) {
            return null;
        }

        String officialArtwork = sprites.getOfficialArtworkUrl();
        if (officialArtwork != null && !officialArtwork.isEmpty()) {
            return officialArtwork;
        }

        return sprites.getFrontDefault();
    }

    public static class Sprites {
        @SerializedName("front_default")
        private String frontDefault;

        private Other other;

        public String getFrontDefault() {
            return frontDefault;
        }

        public String getOfficialArtworkUrl() {
            if (other == null || other.officialArtwork == null) {
                return null;
            }
            return other.officialArtwork.frontDefault;
        }
    }

    public static class Other {
        @SerializedName("official-artwork")
        private OfficialArtwork officialArtwork;
    }

    public static class OfficialArtwork {
        @SerializedName("front_default")
        private String frontDefault;
    }

    public static class TypeSlot {
        private int slot;
        private TypeInfo type;

        public int getSlot() {
            return slot;
        }

        public String getTypeName() {
            if (type == null) {
                return null;
            }
            return type.name;
        }
    }

    public static class TypeInfo {
        private String name;
    }
}
