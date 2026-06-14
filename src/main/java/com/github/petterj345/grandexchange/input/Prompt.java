package com.github.petterj345.grandexchange.input;

/** A pending chat prompt for a player. */
public record Prompt(PromptType type) {

    public static Prompt of(PromptType type) {
        return new Prompt(type);
    }
}
