{
  description = "steno";

  inputs = {
    flake-parts.url = "github:hercules-ci/flake-parts";
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    mynixpkgs.url = "git+https://github.com/dpom/mynixpkgs";
  };

  outputs =
    { flake-parts, mynixpkgs, ... }@inputs:
    flake-parts.lib.mkFlake { inherit inputs; } {
      perSystem =
        {
          config,
          self',
          inputs',
          pkgs,
          system,
          lib,
          ...
        }:
        let
          mypkgs = mynixpkgs.packages.${system};
          python = pkgs.python312Full;
          tkinter = pkgs.python312Packages.tkinter;
        in
          {
            devShells.default = pkgs.mkShell {
              packages = [
                pkgs.babashka
                pkgs.clj-kondo
                pkgs.cljfmt
                pkgs.uv
                python
                tkinter
              ];
              LD_LIBRARY_PATH = lib.makeLibraryPath [
                pkgs.stdenv.cc.cc
                pkgs.libgcc.lib
                pkgs.zlib
                pkgs.libGL
                pkgs.glib.out
              ];
              shellHook = ''
              uv sync
              source .venv/bin/activate
            '';
            };
          };

      systems = [
        "x86_64-linux"
        "aarch64-linux"
        "x86_64-darwin"
        "aarch64-darwin"
      ];
    };
}
