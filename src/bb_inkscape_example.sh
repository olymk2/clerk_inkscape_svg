# Probably worth expanding to check bb exists
# The echo below is just for grabbing the params when developing
# we can interactively develop our plugin using these params
echo $@ > /tmp/inkscape-plugin-params.dump
bb -i bb_inkscape_example.clj $@
