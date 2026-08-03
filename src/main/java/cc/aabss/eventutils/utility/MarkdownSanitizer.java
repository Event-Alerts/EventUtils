// https://github.com/discord-jda/JDA/blob/master/src/main/java/net/dv8tion/jda/api/utils/MarkdownSanitizer.java

/*
 * Copyright 2015 Austin Keener, Michael Ritter, Florian Spieß, and the JDA contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modified by srnyx and EventUtils contributors
 */

package cc.aabss.eventutils.utility;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;


/**
 * Implements an algorithm that can strip or replace markdown in any supplied string.
 */
public class MarkdownSanitizer {
    /** Normal characters that are not special for markdown, ignoring this has no effect */
    public static final int NORMAL = 0;
    /** Bold region such as "**Hello**" */
    public static final int BOLD = 1 << 0;
    /** Italics region for underline such as "_Hello_" */
    public static final int ITALICS_U = 1 << 1;
    /** Italics region for asterisks such as "*Hello*" */
    public static final int ITALICS_A = 1 << 2;
    /** Monospace region such as "`Hello`" */
    public static final int MONO = 1 << 3;
    /** Monospace region such as "``Hello``" */
    public static final int MONO_TWO = 1 << 4;
    /** Codeblock region such as "```Hello```" */
    public static final int BLOCK = 1 << 5;
    /** Spoiler region such as "||Hello||" */
    public static final int SPOILER = 1 << 6;
    /** Underline region such as "__Hello__" */
    public static final int UNDERLINE = 1 << 7;
    /** Strikethrough region such as "~~Hello~~" */
    public static final int STRIKE = 1 << 8;
    /** Quote region such as {@code "> text here"} */
    public static final int QUOTE = 1 << 9;
    /** Quote block region such as {@code ">>> text here"} */
    public static final int QUOTE_BLOCK = 1 << 10;

    private static final int ESCAPED_BOLD = Integer.MIN_VALUE | BOLD;
    private static final int ESCAPED_ITALICS_U = Integer.MIN_VALUE | ITALICS_U;
    private static final int ESCAPED_ITALICS_A = Integer.MIN_VALUE | ITALICS_A;
    private static final int ESCAPED_MONO = Integer.MIN_VALUE | MONO;
    private static final int ESCAPED_MONO_TWO = Integer.MIN_VALUE | MONO_TWO;
    private static final int ESCAPED_BLOCK = Integer.MIN_VALUE | BLOCK;
    private static final int ESCAPED_SPOILER = Integer.MIN_VALUE | SPOILER;
    private static final int ESCAPED_UNDERLINE = Integer.MIN_VALUE | UNDERLINE;
    private static final int ESCAPED_STRIKE = Integer.MIN_VALUE | STRIKE;
    private static final int ESCAPED_QUOTE = Integer.MIN_VALUE | QUOTE;

    private static final Pattern codeLanguage = Pattern.compile("^\\w+\n.*", Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern quote = Pattern.compile("> +.*", Pattern.DOTALL | Pattern.MULTILINE);
    private static final Pattern quoteBlock = Pattern.compile(">>>\\s+\\S.*", Pattern.DOTALL | Pattern.MULTILINE);

    private int ignored;

    public MarkdownSanitizer() {
        this.ignored = NORMAL;
    }

    public MarkdownSanitizer(int ignored) {
        this.ignored = ignored;
    }

    /**
     * Sanitize string with default settings.
     * <br>Same as {@code sanitize(sequence, SanitizationStrategy.REMOVE)}
     *
     * @param  sequence
     *         The string to sanitize
     *
     * @return The sanitized string
     */
    @NotNull
    public static String sanitize(@NotNull String sequence) {
        return new MarkdownSanitizer().compute(sequence);
    }

    /**
     * Specific regions to ignore.
     * <br>Example: {@code new MarkdownSanitizer().withIgnored(MarkdownSanitizer.BOLD | MarkdownSanitizer.UNDERLINE).compute("Hello __world__!")}
     *
     * @param  ignored
     *         The regions to ignore
     *
     * @return The current sanitizer instance with the new ignored regions
     */
    @NotNull
    public MarkdownSanitizer withIgnored(int ignored) {
        this.ignored |= ignored;
        return this;
    }

    private int getRegion(int index, @NotNull String sequence) {
        if (sequence.length() - index >= 3) {
            String threeChars = sequence.substring(index, index + 3);
            switch (threeChars) {
                case "```":
                    return doesEscape(index, sequence) ? ESCAPED_BLOCK : BLOCK;
                case "***":
                    return doesEscape(index, sequence) ? ESCAPED_BOLD | ITALICS_A : BOLD | ITALICS_A;
            }
        }
        if (sequence.length() - index >= 2) {
            String twoChars = sequence.substring(index, index + 2);
            switch (twoChars) {
                case "**":
                    return doesEscape(index, sequence) ? ESCAPED_BOLD : BOLD;
                case "__":
                    return doesEscape(index, sequence) ? ESCAPED_UNDERLINE : UNDERLINE;
                case "~~":
                    return doesEscape(index, sequence) ? ESCAPED_STRIKE : STRIKE;
                case "``":
                    return doesEscape(index, sequence) ? ESCAPED_MONO_TWO : MONO_TWO;
                case "||":
                    return doesEscape(index, sequence) ? ESCAPED_SPOILER : SPOILER;
            }
        }
        char current = sequence.charAt(index);
        return switch (current) {
            case '*' -> doesEscape(index, sequence) ? ESCAPED_ITALICS_A : ITALICS_A;
            case '_' -> doesEscape(index, sequence) ? ESCAPED_ITALICS_U : ITALICS_U;
            case '`' -> doesEscape(index, sequence) ? ESCAPED_MONO : MONO;
            default -> NORMAL;
        };
    }

    private boolean hasCollision(int index, @NotNull String sequence, char c) {
        if (index < 0) {
            return false;
        }
        return index < sequence.length() - 1 && sequence.charAt(index + 1) == c;
    }

    private int findEndIndex(int afterIndex, int region, @NotNull String sequence) {
        if (isEscape(region)) {
            return -1;
        }
        int lastMatch = afterIndex + getDelta(region) + 1;
        while (lastMatch != -1) {
            switch (region) {
                case BOLD | ITALICS_A:
                    lastMatch = sequence.indexOf("***", lastMatch);
                    break;
                case BOLD:
                    lastMatch = sequence.indexOf("**", lastMatch);
                    if (lastMatch != -1
                            && hasCollision(lastMatch + 1, sequence, '*')) // did we find a bold italics tag?
                    {
                        lastMatch += 3;
                        continue;
                    }
                    break;
                case ITALICS_A:
                    lastMatch = sequence.indexOf('*', lastMatch);
                    if (lastMatch != -1 && hasCollision(lastMatch, sequence, '*')) // did we find a bold tag?
                    {
                        if (hasCollision(lastMatch + 1, sequence, '*')) {
                            lastMatch += 3;
                        } else {
                            lastMatch += 2;
                        }
                        continue;
                    }
                    break;
                case UNDERLINE:
                    lastMatch = sequence.indexOf("__", lastMatch);
                    break;
                case ITALICS_U:
                    lastMatch = sequence.indexOf('_', lastMatch);
                    if (lastMatch != -1 && hasCollision(lastMatch, sequence, '_')) // did we find an underline tag?
                    {
                        lastMatch += 2;
                        continue;
                    }
                    break;
                case SPOILER:
                    lastMatch = sequence.indexOf("||", lastMatch);
                    break;
                case BLOCK:
                    lastMatch = sequence.indexOf("```", lastMatch);
                    break;
                case MONO_TWO:
                    lastMatch = sequence.indexOf("``", lastMatch);
                    if (lastMatch != -1 && hasCollision(lastMatch + 1, sequence, '`')) // did we find a codeblock?
                    {
                        lastMatch += 3;
                        continue;
                    }
                    break;
                case MONO:
                    lastMatch = sequence.indexOf('`', lastMatch);
                    if (lastMatch != -1 && hasCollision(lastMatch, sequence, '`')) // did we find a codeblock?
                    {
                        if (hasCollision(lastMatch + 1, sequence, '`')) {
                            lastMatch += 3;
                        } else {
                            lastMatch += 2;
                        }
                        continue;
                    }
                    break;
                case STRIKE:
                    lastMatch = sequence.indexOf("~~", lastMatch);
                    break;
                default:
                    return -1;
            }
            if (lastMatch == -1 || !doesEscape(lastMatch, sequence)) {
                return lastMatch;
            }
            lastMatch++;
        }
        return -1;
    }

    @NotNull
    private String handleRegion(int start, int end, @NotNull String sequence, int region) {
        String resolved = sequence.substring(start, end);
        return switch (region) {
            case BLOCK, MONO, MONO_TWO -> resolved;
            default -> new MarkdownSanitizer(ignored).compute(resolved);
        };
    }

    private int getDelta(int region) {
        return switch (region) {
            case ESCAPED_BLOCK, ESCAPED_BOLD | ITALICS_A, BLOCK, BOLD | ITALICS_A -> 3;
            case ESCAPED_MONO_TWO, ESCAPED_BOLD, ESCAPED_UNDERLINE, ESCAPED_SPOILER, ESCAPED_STRIKE, MONO_TWO, BOLD, UNDERLINE, SPOILER, STRIKE -> 2;
            case ESCAPED_ITALICS_A, ESCAPED_ITALICS_U, ESCAPED_MONO, ESCAPED_QUOTE, ITALICS_A, ITALICS_U, MONO -> 1;
            default -> 0;
        };
    }

    private void applyStrategy(@NotNull String seq, @NotNull StringBuilder builder) {
        if (codeLanguage.matcher(seq).matches()) {
            builder.append(seq.substring(seq.indexOf("\n") + 1));
        } else {
            builder.append(seq);
        }
    }

    private boolean doesEscape(int index, @NotNull String seq) {
        int backslashes = 0;
        for (int i = index - 1; i > -1; i--) {
            if (seq.charAt(i) != '\\') {
                break;
            }
            backslashes++;
        }
        return backslashes % 2 != 0;
    }

    private boolean isEscape(int region) {
        return (Integer.MIN_VALUE & region) != 0;
    }

    private boolean isIgnored(int nextRegion) {
        return (nextRegion & ignored) == nextRegion;
    }

    /**
     * Computes the provided input.
     * <br>Ignores any regions specified with {@link #withIgnored(int)}.
     *
     * @param  sequence
     *         The string to compute
     *
     * @throws java.lang.IllegalArgumentException
     *         If the provided string is null
     *
     * @return The resulting string after applying the computation
     */
    @NotNull
    public String compute(@NotNull String sequence) {
        StringBuilder builder = new StringBuilder();
        String end = handleQuote(sequence);
        if (end != null) {
            return end;
        }

        boolean onlySpacesSinceNewLine = true;
        for (int i = 0; i < sequence.length(); ) {
            int nextRegion = getRegion(i, sequence);
            char c = sequence.charAt(i);
            boolean isNewLine = c == '\n';
            boolean isSpace = c == ' ';
            onlySpacesSinceNewLine = isNewLine || (onlySpacesSinceNewLine && isSpace);

            if (nextRegion == NORMAL) {
                builder.append(sequence.charAt(i++));
                if ((isNewLine || (isSpace && onlySpacesSinceNewLine)) && i < sequence.length()) {
                    String result = handleQuote(sequence.substring(i));
                    if (result != null) {
                        return builder.append(result).toString();
                    }
                }
                continue;
            }

            int endRegion = findEndIndex(i, nextRegion, sequence);
            if (isIgnored(nextRegion) || endRegion == -1) {
                int delta = getDelta(nextRegion);
                for (int j = 0; j < delta; j++) {
                    builder.append(sequence.charAt(i++));
                }
                continue;
            }
            int delta = getDelta(nextRegion);
            applyStrategy(handleRegion(i + delta, endRegion, sequence, nextRegion), builder);
            i = endRegion + delta;
        }
        return builder.toString();
    }

    @Nullable
    private String handleQuote(@NotNull String sequence) {
        // Special handling for quote
        if (!isIgnored(QUOTE) && quote.matcher(sequence).matches()) {
            int start = sequence.indexOf('>');
            if (start < 0) {
                start = 0;
            }
            return compute(sequence.substring(start + 2));

        } else if (!isIgnored(QUOTE_BLOCK) && quoteBlock.matcher(sequence).matches()) {
            return compute(sequence.substring(4));
        }
        return null;
    }
}