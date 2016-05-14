#!/usr/bin/env bash
#
# Generate systemd service file for Bento and write it to
# the standard location for service files

if [ -z "$SERVICE_NAME" ]
then
    SERVICE_NAME=bento
fi

if [ -z "$BENTO_HOME" ]
then
    BENTO_HOME=/opt/$SERVICE_NAME
fi

if [ -z "$BENTO_SH" ]
then
    BENTO_SH=$BENTO_HOME/bin/bentoserver.sh
fi

SERVICE_ENV="Environment=SERVICE_NAME=$SERVICE_NAME"

mkdir -p /usr/lib/systemd/system
cat > /usr/lib/systemd/system/$SERVICE_NAME.service <<EOF
[Unit]
Description=BentoServer
After=syslog.target
After=network.target

[Service]
Type=simple
ExecStart=$BENTO_SH start
ExecStop=$BENTO_SH stop
ExecReload=$BENTO_SH restart
Restart=on-success
User=bento
Group=bento
TimeoutSec=300
$SERVICE_ENV

[Install]
WantedBy=multi-user.target
EOF
