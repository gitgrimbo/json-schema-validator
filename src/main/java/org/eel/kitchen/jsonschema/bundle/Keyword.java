/*
 * Copyright (c) 2012, Francis Galiegue <fgaliegue@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Lesser GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.eel.kitchen.jsonschema.bundle;

import com.google.common.base.Preconditions;
import org.eel.kitchen.jsonschema.keyword.KeywordValidator;
import org.eel.kitchen.jsonschema.syntax.SyntaxChecker;

/**
 * Class used to build a new keyword
 *
 * <p>The process is as follows:</p>
 *
 * <ul>
 *     <li>choose the name of the new keyword;</li>
 *     <li>create a {@link SyntaxChecker} for this keyword;</li>
 *     <li>create a {@link KeywordValidator};</li>
 *     <li>create the keyword.</li>
 * </ul>
 *
 * <p>Sample code:</p>
 *
 * <pre>
 *     final SyntaxChecker checker = ...;
 *     final Class&lt;? extends KeywordValidator&gt; validatorClass = ...;
 *
 *     final Keyword myKeyword = Keyword.Builder.forKeyword("mykeyword")
 *         .withSyntaxChecker(checker).withValidatorClass(validatorClass)
 *         .build();
 *
 *     // register it into a KeywordBundle, see link below
 *     bundle.registerKeyword(myKeyword);
 * </pre>
 *
 * <p>It is perfectly legal to register a keyword with only a syntax checker, or
 * only a keyword validator. An example is {@code $schema}, which must be a
 * valid URI (therefore it has a syntax checker) but does not play any role
 * in instance validation (therefore it has no keyword validator).</p>
 *
 * <p>This class is thread safe and immutable.</p>
 *
 * @see Keyword.Builder
 * @see SyntaxChecker
 * @see KeywordValidator
 * @see KeywordBundle
 */
public final class Keyword
{
    private final String name;
    private final SyntaxChecker syntaxChecker;
    private final Class<? extends KeywordValidator> validatorClass;

    private Keyword(final Builder builder)
    {
        name = builder.keyword;
        syntaxChecker = builder.syntaxChecker;
        validatorClass = builder.validatorClass;
    }

    public String getName()
    {
        return name;
    }

    public SyntaxChecker getSyntaxChecker()
    {
        return syntaxChecker;
    }

    public Class<? extends KeywordValidator> getValidatorClass()
    {
        return validatorClass;
    }

    /**
     * Builder class for a new keyword
     */
    public static final class Builder
    {
        private final String keyword;
        private SyntaxChecker syntaxChecker;
        private Class<? extends KeywordValidator> validatorClass;

        /**
         * The only constructor, private by design
         *
         * @param keyword the keyword name
         * @throws NullPointerException the keyword name is null (illegal)
         */
        private Builder(final String keyword)
        {
            Preconditions.checkNotNull(keyword, "keyword name must not be null");
            this.keyword = keyword;
        }

        /**
         * The one and only static factory method to build an instance
         *
         * @param keyword the keyword to use
         * @return the newly created instance
         */
        public static Builder forKeyword(final String keyword)
        {
            return new Builder(keyword);
        }

        /**
         * Add a syntax checker to this keyword
         *
         * @param syntaxChecker the syntax checker, already instantiated
         * @return this
         */
        public Builder withSyntaxChecker(final SyntaxChecker syntaxChecker)
        {
            this.syntaxChecker = syntaxChecker;
            return this;
        }

        /**
         * Add the keyword validator class
         *
         * <p>We add the class, not an instance, since the generated object is
         * dependent on the schema being passed.</p>
         *
         * @param validatorClass the class
         * @return this
         */
        public Builder withValidatorClass(
            final Class<? extends KeywordValidator> validatorClass)
        {
            this.validatorClass = validatorClass;
            return this;
        }

        /**
         * Build the {@link Keyword}
         *
         * @return the keyword
         */
        public Keyword build()
        {
            return new Keyword(this);
        }
    }
}
