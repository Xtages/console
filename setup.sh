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
    local _bin="$2"
    if [[ -z "$_bin" ]]; then
        _bin="$_pkg"
    fi
    if ! bin_exists "$_bin"; then
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
        local _nvm_dir="$([ -z "${XDG_CONFIG_HOME-}" ] && printf %s "${HOME}/.nvm" || printf %s "${XDG_CONFIG_HOME}/nvm")"
        [ -s "$_nvm_dir/nvm.sh" ] && source "$_nvm_dir/nvm.sh" # This loads nvm
    fi
}

function start_postgress() {
    local _out="$( pg_ctl status -D $(brew --prefix)/var/postgres )"
    echo $_out
    if [[ ! "$_out" == *"server is running"* ]]; then
        pg_ctl start -D $(brew --prefix)/var/postgres
    fi
    if [[ ! "$( psql --dbname=postgres -tAc "SELECT 1 FROM pg_user WHERE usename='xtages_console'" )" == '1' ]]; then
        createuser -s "xtages_console"
    fi
    if [[ ! "$( psql --dbname=postgres -tAc "SELECT 1 FROM pg_database WHERE datname='xtages_console'" )" == '1' ]]; then
        createdb "xtages_console" "Main Xtages console DB" --owner="xtages_console"
    fi
}

if [[ "$OSTYPE" == "darwin"* ]]; then
    install_brew

    brew_install tfenv
    tfenv install

    brew_install postgresql@13 postgres
    start_postgress

    brew_install liquibase

    brew_install "stripe/stripe-cli/stripe" stripe

    brew_install awscli aws

    install_sdkman
    sdk env install

    install_nvm
    nvm install
else
    loge "This script currently only runs on macOS"
fi
