package tech.harmonysoft.oss.inpertio.client.spi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Context {

    /**
     * <p>
     *     Allows answering if given type is a <i>simple type</i> like {@code int}, {@code long} etc.
     * </p>
     * <p>
     *     Properties of that types are processed in a <i>just parse it</i> way:
     *     <ol>
     *         <li>{@link #getRegularPropertyName(String, String) Pick up the property by name}</li>
     *         <li>
     *             {@link #convertIfNecessary(Object, Class)} Normalize the value,
     *             e.g. from {@code String} to {@code int}
     *         </li>
     *     </ol>
     */
    boolean isSimpleType(@NotNull Class<?> clazz);

    /**
     * <p>
     *     Allows answering if given type is a <i>collection type</i>, like {@link List}, {@link Set} etc.
     * </p>
     * <p>
     *     Properties of that type are processed using the following approach:
     *     <ol>
     *         <li>{@link #createCollection(Class) Create target collection}</li>
     *         <li>
     *             Try looking up collection elements using
     *             {@link #getCollectionElementPropertyName(String, int) collection property names} with index starting
     *             from {@code 0}. It's assumed that {@link #isTolerateEmptyCollection()} is {@code false} during that.
     *             So, as soon as a lookup for the given index fails, we assume that all collection elements were discovered
     *         </li>
     *     </ol>
     */
    boolean isCollection(@NotNull Class<?> clazz);

    /**
     * <p>
     *     Suppose we have a config class {@code ListHolder} with a single property {@code List<Integer> list}.
     *     As explained in {@link #isCollection(Class)}, we start from index `0` and while
     *     {@link #getCollectionElementPropertyName(String, int) an element at the target index is found},
     *     add it to the processing. So, we stop as soon as there is no element for the target index.
     * </p>
     * <p>
     *     Consider that we have a class {@code CompositeListHolder} with property {@code List<ListHolder> list}
     *     - we don't know how many {@code ListHolder} elements are in the target collection, so, we start from
     *     index {@code 0} and increment it.
     *     The thing is that we need to look up a {@code List<Integer>} to create {@code ListHolder} and its
     *     perfectly legal to have a {@code ListHolder} with an empty collection. Hence, here we need a way to
     *     understand if we found all {@code ListHolder} elements for {@code CompositeListHolder}. A stop criteria
     *     is a situation when {@code ListHolder} contains an empty collection.
     * </p>
     * <p>
     *     Summarizing, this property defines if an empty collection is considered to be 'ok scenario'
     *     or 'no data scenario'.
     * </p>
     */
    boolean isTolerateEmptyCollection();

    /**
     * <p>
     *     This property complements {@link #isTolerateEmptyCollection()} - there are situations like below:
     * </p>
     * <pre>
     * class Composite{
     *     List&lt;Leaf&gt; leaves;
     * }
     *
     * class Leaf {
     *     int i;
     *    {@literal @}Nullable Set&lt;String&gt; strings;
     * }
     * </pre>
     *
     * <p>
     *     Here we normally have {@link #isTolerateEmptyCollection()} returning {@code false} during
     *     {@code Leaf} objects instantiation. However, as the class has non-nullable parameter and collection parameter
     *     is marked nullable, it's ok to accept a nullable collection.
     * </p>
     * <p>
     *     This property allows answering if there are mandatory parameters in the target class
     * </p>
     */
    boolean hasMandatoryParameter();

    /**
     * A strategy for building property names. Consider the following situation:
     * <pre>
     *     class Inner {
     *         int innerProp;
     *     }
     *     class Outer {
     *         Inner outerProp;
     *     }
     * </pre>
     * <p>
     * Suppose that we initially start with an empty <i>base path</i>. Then it's expected to find an {@code int} value
     * in property {@code 'outerProp.innerProp'} (default property name strategy).
     *
     * @param base         base property path ({@code outerProp} in the example above)
     * @param propertyName target property name ({@code innerProp} in the example above)
     */
    @NotNull
    String getRegularPropertyName(@NotNull String base, @NotNull String propertyName);

    /**
     * A strategy for building collection property names. As explained in [tolerateEmptyCollection] documentation,
     * we start from index `0` and try looking up collection element for the target index.
     * <p>
     * E.g. to build and instance of {@code data class ListHolder(val prop: List&lt;ListElement&gt;)}
     * (where {@code ListElement}` is declared like {@code data class ListElement(val value: Int)}),
     * we start from index `0`, so, by default property name {@code prop[0].value} is used for
     * {@code [looking up property value][getPropertyValue]},
     * then property name {@code prop[1].value} for index {@code 1} etc.
     * <p>
     * This strategy allows building property name for the target index.
     */
    @NotNull
    String getCollectionElementPropertyName(@NotNull String base, int index);

    /**
     * Executes given action in a context where {@link #isTolerateEmptyCollection()} has given value.
     */
    @NotNull
    <T> T withTolerateEmptyCollection(boolean value, @NotNull Supplier<T> action);

    /**
     * Executes given action in a context where [hasMandatoryParameter] returns given value.
     */
    @NotNull
    <T> T withMandatoryParameter(boolean value, @NotNull Supplier<T> action);

    /**
     * Base mandatory method in this interface. Looks up a property value for the given property name.
     *
     * @return {@code null} as an indication that no value is found for the given key; {@code non-null} value otherwise
     */
    @Nullable
    Object getPropertyValue(@NotNull String propertyName);

    /**
     * Allows converting given 'raw value' to the target type, e.g. {@code String} to {@code Int}
     */
    @NotNull
    Object convertIfNecessary(@NotNull Object value, @NotNull Class<?> clazz);

    /**
     * Creates new mutable collection of the given base type. It's assumed that this method supports
     * all types declared in {@link #isCollection(Class)}
     */
    @NotNull
    Collection<Object> createCollection(@NotNull Class<?> klass);

    /**
     * <p>
     *     It's not possible to automatically deduce target keys for {@link Map} property. Hence, the only way is
     *     to configure predefined keys in context and try all of them during instantiation.
     * </p>
     * <p>
     *     This method exposes keys configured for the target type (if any).
     * </p>
     */
    @NotNull
    Set<String> getMapKeys(@NotNull String mapPropertyName, @NotNull Type keyType);

    /**
     * A strategy for building map value property names, e.g. if we have a {@link Map} property named {@code 'prop'}
     * and want to check if there is a value for key {@code ONE}, then we call this method and it might return
     * property name {@code prop.ONE}.
     */
    @NotNull
    String getMapValuePropertyName(@NotNull String base, @NotNull String key);

    /**
     * Creates mutable map to use for a map property.
     */
    Map<Object, Object> createMap();

    interface Builder {

        /**
         * {@link Context#isSimpleType(Class) 'Simple types'} to use.
         *
         * @param types            'simple types' to use
         * @param replaceDefault   defines if given types should be added to the default types or replace them
         */
        @NotNull
        Builder withSimpleTypes(@NotNull Set<Class<?>> types, boolean replaceDefault);

        /**
         * Strategy for converting 'raw values' to the target type, e.g. {@code String} to {@code Int}.
         *
         * @param replace       defines whether given converter should be used as a complement to the
         *                      built-in converter or should completely replace it
         * @param converter     converter to use
         */
        @NotNull
        Builder withTypeConverter(boolean replace, @NotNull BiFunction<Object, Class<?>, Object> converter);

        /**
         * Strategy for building 'regular property' names (as opposed to
         * {@link Context#getCollectionElementPropertyName(String, int) collection property names}
         */
        @NotNull
        Builder withRegularPropertyNameStrategy(@NotNull BiFunction<String, String, String> strategy);

        /**
         * {@link Context#isCollection(Class) Collection types} to use
         *
         * @param types          'collection types' to use
         * @param replaceDefault defines if given types should be added to the default types or replace them
         */
        @NotNull
        Builder withCollectionTypes(@NotNull Set<Class<?>> types, boolean replaceDefault);

        /**
         * Strategy for creating {@link Context#isCollection(Class) 'collection types'}.
         *
         * @param replaceDefault defines if given strategy should be used only if default strategy fails creating
         *                       a collection of the target type
         * @param creator        the actual strategy; returns `null` as an indication that it doesn't know how to
         *                       create a collection of the target type
         */
        @NotNull
        Builder withCollectionCreator(boolean replaceDefault, @NotNull Function<Class<?>, Collection<Object>> creator);

        /**
         * Strategy for creating {@link Context#getCollectionElementPropertyName(String, int) collection property name}
         */
        @NotNull
        Builder withCollectionElementPropertyNameStrategy(@NotNull BiFunction<String, Integer, String> strategy);

        /**
         * Strategy for map property creator.
         */
        @NotNull
        Builder withMapCreator(@NotNull Supplier<Map<Object, Object>> creator);

        /**
         * Strategy for keys to try for the target key type (see {@link Context#getMapKeys(String, Type)}).
         *
         * @param replaceDefault defines whether given strategy should be used as a complement to the
         *                       built-in strategy or should completely replace it
         * @param strategy       the actual strategy; returns empty set as an indication that it doesn't
         *                       know how to map target keys
         */
        @NotNull
        Builder withMapKeyStrategy(boolean replaceDefault, @NotNull BiFunction<String, Type, Set<String>> strategy);

        /**
         * Strategy for map value property name strategy (see {@link Context#getMapValuePropertyName(String, String)}.
         */
        @NotNull
        Builder withMapValuePropertyNameStrategy(@NotNull BiFunction<String, String, String> strategy);

        @NotNull
        Context build();
    }
}