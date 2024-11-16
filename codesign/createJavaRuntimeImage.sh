rm -rf java-runtime
jlink --add-modules ALL-MODULE-PATH --strip-debug --no-man-pages --no-header-files  --output java-runtime
rm -rf java-runtime/lib/fontconfig.bfc
cp java-runtime/lib/fontconfig.properties.src java-runtime/lib/fontconfig.properties
cat <<EOF >> java-runtime/lib/fontconfig.properties

# ignore invalid characters
exclusion.dingbats=0100-10ffff
exclusion.symbol=0100-10ffff
EOF

