;;; .ent.el --- local ent config file -*- lexical-binding: t; -*-

;;; commentary:

;;; code:

;; project settings
(setq ent-project-home (file-name-directory (if load-file-name load-file-name buffer-file-name)))
(setq ent-project-name "cljproj")
(setq ent-clean-regexp "~$\\|\\.tex$")

(require 'ent)

(ent-tasks-init)

(task 'style '() "check code" '(lambda (&optional x) "bb style"))

(task 'format '() "format code" '(lambda (&optional x) "bb format"))

(task 'kondo '() "lint with kondo" '(lambda (&optional x) "bb kondo"))

(task 'libupdate '() "search for new libs versions" '(lambda (&optional x) "bb libupdate" ))

(task 'tests '() "run tests" '(lambda (&optional x) "bb test"))

(task 'readme '() "build readme file" '(lambda (&optional x) "pandoc -o README.md tmp/README.org"))

(provide '.ent)
;;; .ent.el ends here

;; local variables:
;; no-byte-compile: t
;; no-update-autoloads: t
;; end:
