package ru.aokazantsev.devatlas.themeformat.svg

enum class ColorRole(val cssName: String) {
    PRIMARY("primary"),
    ON_PRIMARY("on-primary"),
    PRIMARY_CONTAINER("primary-container"),
    ON_PRIMARY_CONTAINER("on-primary-container"),
    INVERSE_PRIMARY("inverse-primary"),
    SECONDARY("secondary"),
    ON_SECONDARY("on-secondary"),
    SECONDARY_CONTAINER("secondary-container"),
    ON_SECONDARY_CONTAINER("on-secondary-container"),
    TERTIARY("tertiary"),
    ON_TERTIARY("on-tertiary"),
    TERTIARY_CONTAINER("tertiary-container"),
    ON_TERTIARY_CONTAINER("on-tertiary-container"),
    BACKGROUND("background"),
    ON_BACKGROUND("on-background"),
    SURFACE("surface"),
    ON_SURFACE("on-surface"),
    SURFACE_VARIANT("surface-variant"),
    ON_SURFACE_VARIANT("on-surface-variant"),
    SURFACE_TINT("surface-tint"),
    INVERSE_SURFACE("inverse-surface"),
    INVERSE_ON_SURFACE("inverse-on-surface"),
    ERROR("error"),
    ON_ERROR("on-error"),
    ERROR_CONTAINER("error-container"),
    ON_ERROR_CONTAINER("on-error-container"),
    OUTLINE("outline"),
    OUTLINE_VARIANT("outline-variant"),
    SCRIM("scrim"),
    SURFACE_BRIGHT("surface-bright"),
    SURFACE_DIM("surface-dim"),
    SURFACE_CONTAINER("surface-container"),
    SURFACE_CONTAINER_HIGH("surface-container-high"),
    SURFACE_CONTAINER_HIGHEST("surface-container-highest"),
    SURFACE_CONTAINER_LOW("surface-container-low"),
    SURFACE_CONTAINER_LOWEST("surface-container-lowest"),
    PRIMARY_FIXED("primary-fixed"),
    PRIMARY_FIXED_DIM("primary-fixed-dim"),
    ON_PRIMARY_FIXED("on-primary-fixed"),
    ON_PRIMARY_FIXED_VARIANT("on-primary-fixed-variant"),
    SECONDARY_FIXED("secondary-fixed"),
    SECONDARY_FIXED_DIM("secondary-fixed-dim"),
    ON_SECONDARY_FIXED("on-secondary-fixed"),
    ON_SECONDARY_FIXED_VARIANT("on-secondary-fixed-variant"),
    TERTIARY_FIXED("tertiary-fixed"),
    TERTIARY_FIXED_DIM("tertiary-fixed-dim"),
    ON_TERTIARY_FIXED("on-tertiary-fixed"),
    ON_TERTIARY_FIXED_VARIANT("on-tertiary-fixed-variant");

    companion object {
        private val byCssName = entries.associateBy { it.cssName }

        fun fromCssName(name: String): ColorRole? = byCssName[name]
    }
}
