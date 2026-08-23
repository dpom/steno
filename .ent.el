;;; .ent.el --- local ent config file -*- lexical-binding: t; -*-

;;; commentary:

;;; code:

;; project settings
(setq ent-project-home (file-name-directory (if load-file-name load-file-name buffer-file-name)))
(setq ent-project-name "steno")
(setq ent-clean-regexp ".*~$\\|.*sync-conflict.*$")
(setq ent-dirclean-regexp "__pycache__" )

(ent-load-default-tasks)

;; tasks


(provide '.ent)
;;; .ent.el ends here

;; local variables:
;; no-byte-compile: t
;; no-update-autoloads: t
;; end:
