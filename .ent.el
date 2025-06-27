;;; .ent.el --- local ent config file -*- lexical-binding: t; -*-

;;; commentary:

;;; code:

;; project settings
(setq ent-project-home (file-name-directory (if load-file-name load-file-name buffer-file-name)))
(setq ent-project-name "cljproj")
(setq ent-clean-regexp "~$")
(setq ent-dirclean-regexp "__pycache__" )



(task :style '() "check code" "bb style")

(task :format '() "format code" "bb format")

(task :kondo '() "lint with kondo" "bb kondo")

(task :libupdate '() "search for new libs versions" "bb libupdate" )

(task :tests '() "run tests" "bb test")


(provide '.ent)
;;; .ent.el ends here

;; local variables:
;; no-byte-compile: t
;; no-update-autoloads: t
;; end:
