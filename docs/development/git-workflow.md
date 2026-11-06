# Git workflow

Only repository root has `.git`; no submodules/nested repos. Bootstrap originally initialized
main; the user has since committed the foundation and configured origin. Preserve existing
history/remotes and unrelated working-tree changes. Create task branches: `git switch -c feat/<short-name>`.
Review `git status` and `git diff` before staging; add explicit intended paths. Keep STL
assets intact. Never add `.env`, `.local`, `.venv`, build output or personal logs.

PR describes problem, final change and evidence. One other team member reviews. Require
`CI / required` once hosting is configured. Workflow always starts; jobs use change filters,
with a final always-running gate so skipped components do not leave a pending required
check. Contracts changes run all consumers. Merge only after relevant checks pass.
No remote push, production deploy or GitHub repository creation is part of bootstrap.
