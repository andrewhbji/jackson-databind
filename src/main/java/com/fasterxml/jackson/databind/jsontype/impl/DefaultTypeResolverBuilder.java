package com.fasterxml.jackson.databind.jsontype.impl;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.core.TreeNode;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Customized {@link TypeResolverBuilder} that provides type resolver builders
 * used with so-called "default typing"
 * (see {@link com.fasterxml.jackson.databind.cfg.MapperBuilder#activateDefaultTyping(PolymorphicTypeValidator)}
 * for details).
 *<p>
 * Type resolver construction is based on configuration: implementation takes care
 * of only providing builders in cases where type information should be applied.
 * This is important since build calls may be sent for any and all types, and
 * type information should NOT be applied to all of them.
 */
public class DefaultTypeResolverBuilder
    extends StdTypeResolverBuilder
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * Validator to use for checking that only valid subtypes are accepted
     * from incoming content.
     */
    protected final PolymorphicTypeValidator _subtypeValidator;

    /**
     * Definition of what types is this default typer valid for.
     */
    protected final DefaultTyping _appliesFor;

    public DefaultTypeResolverBuilder(PolymorphicTypeValidator subtypeValidator,
            DefaultTyping t, JsonTypeInfo.As includeAs) {
        _subtypeValidator = subtypeValidator;
        _appliesFor = t;
        _idType = JsonTypeInfo.Id.CLASS;
        _includeAs = includeAs;
        _typeProperty = _idType.getDefaultPropertyName();
    }

    public DefaultTypeResolverBuilder(PolymorphicTypeValidator subtypeValidator,
            DefaultTyping t, String propertyName) {
        _subtypeValidator = subtypeValidator;
        _appliesFor = t;
        _idType = JsonTypeInfo.Id.CLASS;
        _includeAs = JsonTypeInfo.As.PROPERTY;
        _typeProperty = propertyName;
    }

    public DefaultTypeResolverBuilder(PolymorphicTypeValidator subtypeValidator,
            DefaultTyping t, JsonTypeInfo.As includeAs,
            JsonTypeInfo.Id idType, String propertyName) {
        _subtypeValidator = subtypeValidator;
        _appliesFor = t;
        _idType = idType;
        _includeAs = includeAs;
        if (propertyName == null) {
            propertyName = _idType.getDefaultPropertyName();
        }
        _typeProperty = propertyName;
    }

    protected DefaultTypeResolverBuilder(DefaultTypeResolverBuilder base,
            Class<?> defaultImpl) {
        super(base, defaultImpl);
        _subtypeValidator = base._subtypeValidator;
        _appliesFor = base._appliesFor;
    }

    @Override
    public DefaultTypeResolverBuilder withDefaultImpl(Class<?> defaultImpl) {
        if (_defaultImpl == defaultImpl) {
            return this;
        }
        ClassUtil.verifyMustOverride(DefaultTypeResolverBuilder.class, this, "withDefaultImpl");

        // NOTE: MUST create new instance, NOT modify this instance
        return new DefaultTypeResolverBuilder(this, defaultImpl);
    }

    @Override
    public PolymorphicTypeValidator subTypeValidator(DatabindContext ctxt) {
        return _subtypeValidator;
    }

    @Override
    public TypeDeserializer buildTypeDeserializer(DeserializationContext ctxt,
            JavaType baseType, Collection<NamedType> subtypes)
    {
        return useForType(baseType) ? super.buildTypeDeserializer(ctxt, baseType, subtypes) : null;
    }

    @Override
    public TypeSerializer buildTypeSerializer(SerializerProvider ctxt,
            JavaType baseType, Collection<NamedType> subtypes)
    {
        return useForType(baseType) ? super.buildTypeSerializer(ctxt, baseType, subtypes) : null;            
    }

    public DefaultTypeResolverBuilder typeIdVisibility(boolean isVisible) {
        _typeIdVisible = isVisible;
        return this;
    }

    /**
     * Method called to check if the default type handler should be
     * used for given type.
     * Note: "natural types" (String, Boolean, Integer, Double) will never
     * use typing; that is both due to them being concrete and final,
     * and since actual serializers and deserializers will also ignore any
     * attempts to enforce typing.
     */
    public boolean useForType(JavaType t)
    {
        // Need to skip primitive types too, regardless
        if (t.isPrimitive()) {
            return false;
        }

        switch (_appliesFor) {
        case NON_CONCRETE_AND_ARRAYS:
            while (t.isArrayType()) {
                t = t.getContentType();
            }
            // fall through
        case OBJECT_AND_NON_CONCRETE:
            // 19-Apr-2016, tatu: ReferenceType like Optional also requires similar handling:
            while (t.isReferenceType()) {
                t = t.getReferencedType();
            }
            return t.isJavaLangObject()
                    || (!t.isConcrete()
                            // [databind#88] Should not apply to JSON tree models:
                            && !TreeNode.class.isAssignableFrom(t.getRawClass()));

        case NON_FINAL:
            while (t.isArrayType()) {
                t = t.getContentType();
            }
            // ReferenceType like Optional also requires similar handling:
            while (t.isReferenceType()) {
                t = t.getReferencedType();
            }
            // [databind#88] Should not apply to JSON tree models:
            return !t.isFinal() && !TreeNode.class.isAssignableFrom(t.getRawClass());

        case EVERYTHING:
            // So, excluding primitives (handled earlier) and "Natural types" (handled
            // before this method is called), applied to everything
            return true;

        default:
        case JAVA_LANG_OBJECT:
            return t.isJavaLangObject();
        }
    }
}