#!/bin/bash

cd $HOME
echo ""
echo "=================================================="
echo "Pre-config : Installing cpanminus..."
echo "=================================================="
echo ""
curl -L https://cpanmin.us | perl - App::cpanminus

export PATH=$PATH:$HOME/perl5/bin

echo ""
echo "=================================================="
echo "Pre-config : Installing defects4j..."
echo "=================================================="
echo ""
git clone https://github.com/rjust/defects4j
cd defects4j
cpanm --installdeps . && ./init.sh
export PATH=$PATH:$HOME/defects4j/framework/bin
echo "export PATH=$PATH:$HOME/defects4j/framework/bin" >> ~/.bashrc

cd $HOME
echo ""
echo "=================================================="
echo "Pre-config : Installing sdkman..."
echo "=================================================="
echo ""
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

echo ""
echo "=================================================="
echo "Pre-config : Installing SDKs: Gradle and Maven..."
echo "=================================================="
echo ""
sdk install maven
sdk install gradle 7.4

echo ""
echo "=================================================="
echo "Pre-config : Installing JDKs: JDK 8 and JDK 17..."
echo "=================================================="
echo ""
# In case those two below are not found, use "sdk list java" to find the versions for those two.
# Installing more than two versions of the same software will ask to select the default version. For now, set JDK 17 as default.
sdk install java 17.0.5-oracle
sdk install java 8.0.352-tem

echo ""
echo "=================================================="
echo "Pre-config : Installing Python 3 package(s)..."
echo "=================================================="
echo ""
pip3 install jproperties

echo ""
echo "=================================================="
echo "Pre-config : Installation Completed"
echo "=================================================="
echo ""
