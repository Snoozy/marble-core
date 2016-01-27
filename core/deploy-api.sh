#!/bin/bash

echo 'Generating package...'

pack_path=$(activator dist | tee /dev/tty | awk '/package is ready/ {print $7;}')

package_path=${pack_path%????}

fullfile=$(basename ${package_path})

filename=${fullfile%????}

echo "Package is located at ${package_path}"

server_ips="52.25.154.110"

echo 'Starting deployment...'

if [ -e "$package_path" ]; then
    for server in "$server_ips"
    do
        echo "Deploying to ${server}."
        scp "$package_path" ubuntu@${server}:/home/ubuntu/marble-api.zip
        rsync -avz $HOME/.marble/prod_api.conf ubuntu@${server}:/home/ubuntu/
        ssh ubuntu@${server} bash -c "'
            unzip -o marble-api.zip
            sudo rm -rf marble-api-backup/
            mv marble-api/ marble-api-backup/
            mv ${filename}/ marble-api/
            chmod 755 ./marble-api/bin/marble
            rm marble-api.zip
            sudo kill \$(head -n 1 /home/ubuntu/marble-api-backup/RUNNING_PID)
            sudo ./marble-api/bin/marble -J-Xms128M -J-Xmx750M -J-server -Dconfig.file=/home/ubuntu/prod_api.conf -Dhttp.port=80 &
            disown
        '"
    done
else
    echo 'Deployment error.'
fi