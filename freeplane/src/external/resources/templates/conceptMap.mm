<map version="freeplane 1.9.8">
<!--To view this file, download free mind mapping software Freeplane from https://www.freeplane.org -->
<node TEXT="New Concept Map" STYLE="oval">
<hook NAME="MapStyle" max_node_width="600">
<properties auto_compact_layout="true" edgeColorConfiguration="#808080ff,#ff0000ff,#0000ffff,#00ff00ff,#ff00ffff,#00ffffff,#7c0000ff,#00007cff,#007c00ff,#7c007cff,#007c7cff,#7c7c00ff"/>
    <conditional_styles>
        <conditional_style ACTIVE="true" LOCALIZED_STYLE_REF="styles.connection">
            <node_periodic_level_condition PERIOD="2" REMAINDER="1"/>
        </conditional_style>
        <conditional_style ACTIVE="true" LOCALIZED_STYLE_REF="styles.topic">
            <node_level_condition VALUE="2" IGNORE_CASE="true" COMPARATION_RESULT="0" SUCCEED="true"/>
        </conditional_style>
        <conditional_style ACTIVE="true" LOCALIZED_STYLE_REF="styles.subtopic">
            <node_level_condition VALUE="4" IGNORE_CASE="true" COMPARATION_RESULT="0" SUCCEED="true"/>
        </conditional_style>
        <conditional_style ACTIVE="true" LOCALIZED_STYLE_REF="styles.subsubtopic">
            <node_level_condition VALUE="6" IGNORE_CASE="true" COMPARATION_RESULT="0" SUCCEED="true"/>
        </conditional_style>
    </conditional_styles>
	<map_styles>
		<stylenode LOCALIZED_TEXT="styles.root_node" STYLE="circle" >
		<stylenode LOCALIZED_TEXT="styles.predefined" POSITION="right">
		<stylenode LOCALIZED_TEXT="default" COLOR="#000000" STYLE="fork" ICON_SIZE="12.0 pt">
		<font NAME="Arial" SIZE="10" BOLD="false" ITALIC="false"/>
		<richcontent CONTENT-TYPE="plain/auto" TYPE="DETAILS"/>
		<richcontent TYPE="NOTE" CONTENT-TYPE="plain/auto"/>
		</stylenode>
		<stylenode LOCALIZED_TEXT="defaultstyle.details"/>
		<stylenode LOCALIZED_TEXT="defaultstyle.note" COLOR="#000000" BACKGROUND_COLOR="#ffffff" TEXT_ALIGN="LEFT" />
		<stylenode LOCALIZED_TEXT="defaultstyle.floating">
		<edge STYLE="hide_edge"/>
		<cloud COLOR="#f0f0f0" SHAPE="ROUND_RECT"/>
		</stylenode>
		<stylenode LOCALIZED_TEXT="defaultstyle.selection" BACKGROUND_COLOR="#afd3f7" BORDER_COLOR_LIKE_EDGE="false" BORDER_COLOR="#afd3f7"/>
		</stylenode>
		<stylenode LOCALIZED_TEXT="styles.user-defined" POSITION="right">
		<stylenode LOCALIZED_TEXT="styles.topic" COLOR="#18898b" STYLE="fork">
		<font NAME="Liberation Sans" SIZE="10" BOLD="true"/>
		</stylenode>
		<stylenode LOCALIZED_TEXT="styles.subtopic" COLOR="#cc3300" STYLE="fork">
		<font NAME="Liberation Sans" SIZE="10" BOLD="true"/>
		</stylenode>
		<stylenode LOCALIZED_TEXT="styles.subsubtopic" COLOR="#669900">
		<font NAME="Liberation Sans" SIZE="10" BOLD="true"/>
		</stylenode>
		<stylenode LOCALIZED_TEXT="styles.connection" COLOR="#606060" STYLE="fork">
		<font NAME="Arial" SIZE="8" BOLD="false"/>
		</stylenode>
		</stylenode>
		<stylenode LOCALIZED_TEXT="styles.AutomaticLayout" POSITION="right">
		<stylenode LOCALIZED_TEXT="AutomaticLayout.level.root" COLOR="#000000" STYLE="oval">
		<font SIZE="18"/>
		</stylenode>
		<stylenode LOCALIZED_TEXT="AutomaticLayout.level,1" COLOR="#0033ff">
		<font SIZE="16"/>
		</stylenode>
		<stylenode LOCALIZED_TEXT="AutomaticLayout.level,2" COLOR="#00b439">
		<font SIZE="14"/>
		</stylenode>
		<stylenode LOCALIZED_TEXT="AutomaticLayout.level,3" COLOR="#990000">
		<font SIZE="12"/>
		</stylenode>
		<stylenode LOCALIZED_TEXT="AutomaticLayout.level,4" COLOR="#111111">
		<font SIZE="10"/>
		</stylenode>
		</stylenode>
		</stylenode>
	</map_styles>
</hook>
</node>
</map>
