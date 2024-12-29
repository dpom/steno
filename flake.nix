{
  description = "Template for basilisp projects";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-parts.url = "github:hercules-ci/flake-parts";
    
    pyproject-nix = {
      url = "github:pyproject-nix/pyproject.nix";
      inputs.nixpkgs.follows = "nixpkgs";
    };

    uv2nix = {
      url = "github:pyproject-nix/uv2nix";
      inputs.pyproject-nix.follows = "pyproject-nix";
      inputs.nixpkgs.follows = "nixpkgs";
    };

    pyproject-build-systems = {
      url = "github:pyproject-nix/build-system-pkgs";
      inputs.pyproject-nix.follows = "pyproject-nix";
      inputs.uv2nix.follows = "uv2nix";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    mynixpkgs.url = "git+https://github.com/dpom/mynixpkgs";
  };

  outputs =
    {
      nixpkgs,
      uv2nix,
      pyproject-nix,
      pyproject-build-systems,
      mynixpkgs,
      flake-parts,
      ...
    } @ inputs:
    flake-parts.lib.mkFlake { inherit inputs;}
      {
        debug = true;
        systems = [ "x86_64-linux" "aarch64-linux" "x86_64-darwin" "aarch64-darwin" ];
        perSystem = { config, self', inputs', pkgs, system, ... }:
          let
            inherit (nixpkgs) lib;

            # Load a uv workspace from a workspace root.
            # Uv2nix treats all uv projects as workspace projects.
            workspace = uv2nix.lib.workspace.loadWorkspace { workspaceRoot = ./.; };

            # Create package overlay from workspace.
            overlay = workspace.mkPyprojectOverlay {
              # Prefer prebuilt binary wheels as a package source.
              # Binary wheels are more likely to, but may still require overrides for library dependencies.
              sourcePreference = "wheel"; # or sourcePreference = "sdist";
              # Optionally customise PEP 508 environment
              # environ = {
              #   platform_release = "5.10.65";
              # };
            };

            # Extend generated overlay with build fixups
            #
            # Uv2nix can only work with what it has, and uv.lock is missing essential metadata to perform some builds.
            # This is an additional overlay implementing build fixups.
            # See:
            # - https://pyproject-nix.github.io/uv2nix/FAQ.html
            pyprojectOverrides = _final: _prev: {
              # Implement build fixups here.
            };

            mypkgs = mynixpkgs.packages.${system};
            
            # Use Python 3.12 from nixpkgs
            python = pkgs.python312;

            # Construct package set
            pythonSet =
              # Use base package set from pyproject.nix builders
              (pkgs.callPackage pyproject-nix.build.packages {
                inherit python;
              }).overrideScope
                (
                  lib.composeManyExtensions [
                    pyproject-build-systems.overlays.default
                    overlay
                    pyprojectOverrides
                  ]
                );

          in
            {
              # Package a virtual environment as our main application.
              #
              # Enable no optional dependencies for production build.
              packages = pythonSet.mkVirtualEnv "steno-env" workspace.deps.default;
              # This devShell simply adds Python and undoes the dependency leakage done by Nixpkgs Python infrastructure.
              devShells.default = pkgs.mkShell {
                packages = [
                  mypkgs.cljstyle
                  pkgs.babashka
                  pkgs.clj-kondo
                  pkgs.uv
                  pkgs.ruff
                  python
                ];
                LD_LIBRARY_PATH = lib.makeLibraryPath [
                  pkgs.stdenv.cc.cc
                  pkgs.libgcc.lib
                  pkgs.zlib
                  pkgs.libGL
                  pkgs.glib.out
                ];
                shellHook = ''
            unset PYTHONPATH
            export UV_PYTHON_DOWNLOADS=never
            uv sync
            source .venv/bin/activate
          '';
              };

            };
      };
}
