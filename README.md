# About the App
Aurora is a personal finance app for budget planning, accounting, and money management.
# Links
[Google Play](https://play.google.com/store/apps/details?id=tech.aurorafin.aurora)

[Official Website](https://aurorafin.tech/)

[Video Usage Guide](https://www.youtube.com/channel/UCv7HSAvYgXRpNxzhkIGejzw/playlists)

# Main components
The app consists of beautiful, specially designed, and well-optimized UI components that allow users to manage their finances conventionally and effectively.

## Toolbar curtain
An amazing interface that is used to locate filter options and data management functions. The solution is designed to deal with the small size of mobile screens. The most used controls can be placed on a toolbar which is always visible. The curtain can be shown and hidden by gesture and it’s followed by beautiful animation.

Layout implementation is in the [fragment_plan.xm](/app/src/main/res/layout/fragment_plan.xml#L158)
Animation and motion controller implementation in [PlanFragment.java](/app/src/main/java/tech/aurorafin/aurora/PlanFragment.java#L564)

## Table with expand/collapse rows
Great interface solution to present monthly or weekly aggregated financial data on a mobile screen. Each row shows subtotal for a period and can be expanded to days or single transactions. The table is well optimized, and animations are running smoothly even on old hardware devices.

The table is based on android standard RecyclerView with custom adapters for main and sub rows. 

Expandable row adapter implementation is in [PlanTableAdapter.java](/app/src/main/java/tech/aurorafin/aurora/PlanTableAdapter.java#L268) 
Sub row adapter example is in the [PlanTableSubRowAdapter.java](/app/src/main/java/tech/aurorafin/aurora/PlanTableSubRowAdapter.java)

## Waterfall chart
One of the most powerfull charts in finance is a waterfall chart. Made of primitive views the chart automatically calculates the best layout to show the most valuable insides of financial data.

The chart is implemented in the [Analysis fragment](/app/src/main/java/tech/aurorafin/aurora/AnalysisFragment.java#L1140)

## Income Statement table
The table with expandable rows serves as a data sourse for the waterfall chart above.
An implementation also is in the [AnalysisFragment.java](/app/src/main/java/tech/aurorafin/aurora/AnalysisFragment.java#L1079)

# License
[GNU General Public License v3.0](https://choosealicense.com/licenses/gpl-3.0/)
