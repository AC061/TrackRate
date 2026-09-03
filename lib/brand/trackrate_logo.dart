import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';

import 'brand_assets.dart';

enum TrackRateLogoVariant { icon, full }

/// TrackRate brand mark migrated from the Android app.
///
/// Use [TrackRateLogoVariant.icon] for compact toolbars and
/// [TrackRateLogoVariant.full] for login and primary navigation.
class TrackRateLogo extends StatelessWidget {
  const TrackRateLogo({
    super.key,
    this.variant = TrackRateLogoVariant.full,
    this.width,
    this.height,
    this.color,
  });

  const TrackRateLogo.icon({
    super.key,
    this.width,
    this.height,
    this.color,
  }) : variant = TrackRateLogoVariant.icon;

  const TrackRateLogo.full({
    super.key,
    this.width,
    this.height,
    this.color,
  }) : variant = TrackRateLogoVariant.full;

  final TrackRateLogoVariant variant;
  final double? width;
  final double? height;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    final asset = switch (variant) {
      TrackRateLogoVariant.icon => BrandAssets.logoIcon,
      TrackRateLogoVariant.full => BrandAssets.logoFull,
    };

    final resolvedWidth = width ?? switch (variant) {
      TrackRateLogoVariant.icon => BrandAssets.iconWidth,
      TrackRateLogoVariant.full => BrandAssets.fullWidth,
    };

    final resolvedHeight = height ?? switch (variant) {
      TrackRateLogoVariant.icon => BrandAssets.iconHeight,
      TrackRateLogoVariant.full => BrandAssets.fullHeight,
    };

    return Semantics(
      label: 'TrackRate',
      image: true,
      child: SvgPicture.asset(
        asset,
        width: resolvedWidth,
        height: resolvedHeight,
        colorFilter: color == null
            ? null
            : ColorFilter.mode(color!, BlendMode.srcIn),
      ),
    );
  }
}

/// Icon + title row used in secondary toolbars, matching Android branding.
class TrackRateBranding extends StatelessWidget {
  const TrackRateBranding({
    super.key,
    required this.title,
    this.iconColor,
    this.titleStyle,
  });

  final String title;
  final Color? iconColor;
  final TextStyle? titleStyle;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        TrackRateLogo.icon(
          width: BrandAssets.toolbarIconSize,
          height: BrandAssets.toolbarIconSize,
          color: iconColor ?? theme.colorScheme.onPrimary,
        ),
        const SizedBox(width: 8),
        Flexible(
          child: Text(
            title,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: titleStyle ??
                theme.textTheme.titleLarge?.copyWith(
                  color: theme.colorScheme.onPrimary,
                ),
          ),
        ),
      ],
    );
  }
}
