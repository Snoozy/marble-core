#!/bin/bash

echo 'Generating package...'

pack_path=$(activator dist | tee /dev/tty | awk '/package is ready/ {print $7;}')

package_path=${pack_path%????}

fullfile=$(basename ${package_path})

filename=${fullfile%????}

echo "Package is located at ${package_path}"

server_ips="54.152.66.222"

echo 'Starting deployment...'

if [ -e "$package_path" ]; then
    prefix=$(cat /dev/urandom | env LC_CTYPE=C tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1)
    tmp=$(mktemp -dt "$0")
    cat ../core/public/desktop/js/*.js > ${tmp}/bundle-desktop.js
    cat ../core/public/desktop/css/*.css > ${tmp}/bundle-desktop.css
    cat ../core/public/mobile/js/*.js > ${tmp}/bundle-mobile.js
    cat ../core/public/mobile/css/*.css > ${tmp}/bundle-mobile.css
    yuicompressor ${tmp}/bundle-desktop.css > ${tmp}/bundle-desktop.min.css
    yuicompressor ${tmp}/bundle-desktop.js > ${tmp}/bundle-desktop.min.js
    yuicompressor ${tmp}/bundle-mobile.css > ${tmp}/bundle-mobile.min.css
    yuicompressor ${tmp}/bundle-mobile.js > ${tmp}/bundle-mobile.min.js
    ../scripts/upload_static  ${tmp}/bundle-desktop.min.js -js "bundle-desktop-${prefix}.js"
    ../scripts/upload_static ${tmp}/bundle-desktop.min.css -css "bundle-desktop-${prefix}.css"
    ../scripts/upload_static  ${tmp}/bundle-mobile.min.js -js "bundle-mobile-${prefix}.js"
    ../scripts/upload_static ${tmp}/bundle-mobile.min.css -css "bundle-mobile-${prefix}.css"
    cp $HOME/.marble/prod_web.conf ${tmp}
    echo "static.prefix=\"${prefix}\"" >> ${tmp}/prod_web.conf
    for server in "$server_ips"
    do
        echo "Deploying to ${server}."
        scp "$package_path" ubuntu@${server}:/home/ubuntu/marble-web.zip
        rsync -avz ${tmp}/prod_web.conf ubuntu@${server}:/home/ubuntu/
        ssh ubuntu@${server} bash -c "'
            unzip -o marble-web.zip
            sudo rm -rf marble-web-backup/
            mv marble-web/ marble-web-backup/
            mv ${filename}/ marble-web/
            chmod 755 ./marble-web/bin/marble
            rm marble-web.zip
            kill \$(head -n 1 /home/ubuntu/marble-web-backup/RUNNING_PID)
            ./marble-web/bin/marble -J-Xms128M -J-Xmx750M -J-server -Dconfig.file=/home/ubuntu/prod_web.conf &
            disown
        '"
    done
else
    echo 'Deployment error.'
fi
