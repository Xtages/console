#! /bin/bash

set -eo pipefail

function loge() { 
    printf "%s\n" "$*" >&2;
}

function logi() { 
    printf "%s\n" "$*";
}

## Checks a the binary exists
function bin_exists() {
    command -v "$1" >/dev/null 2>&1
}

## Installs homebrew if necessary 
function install_brew() {
    local _bin="brew"
    if bin_exists "$_bin"; then
        _version=$( "$_bin" --version )
        logi "$_version"
    else
        /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    fi
}

## Installs brew packages if they are not installed
function brew_install() {
    local _pkg="$1"
    if ! bin_exists "$_pkg"; then
        brew install "$_pkg"
    fi
}

## Installs skdman if necessary
function install_sdkman() {
    local _bin="sdk"
    if [[ -e "$HOME/.sdkman/bin/sdkman-init.sh" ]]; then
        source "$HOME/.sdkman/bin/sdkman-init.sh"
    fi
    if bin_exists "$_bin"; then
        _version=$( "$_bin" version )
        logi "$_version"
    else
        curl -s "https://get.sdkman.io" | bash
        source "$HOME/.sdkman/bin/sdkman-init.sh"
    fi
    logi "Make sure to add source \"$HOME/.sdkman/bin/sdkman-init.sh\" to your .zshrc"
}

## Installs nvm if necessary
function install_nvm() {
    local _bin="nvm"
    if bin_exists "$_bin"; then
        _version=$( "$_bin" --version )
        logi "$_version"
    else
        /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/nvm-sh/nvm/v0.37.2/install.sh)"
    fi
}

if [[ "$OSTYPE" == "darwin"* ]]; then
    install_brew

    brew_install tfenv
    tfenv install

    install_sdkman
    sdk env

    install_nvm
    nvm install
else
    loge "This script currently only runs on macOS"
fi