#!/bin/bash
# ─────────────────────────────────────────────────────────────
# CLEAN PUSH - Only essential production files
# ─────────────────────────────────────────────────────────────

set -e

cd /workspaces/JAVA_COURSE_RECOMMEDER

echo ""
echo "🧹 Cleaning up unnecessary files..."
echo ""

# Remove helper scripts
rm -f quickstart.sh start.sh seed.sh deploy.sh build_and_run.sh local_setup.sh
echo "✅ Removed helper scripts"

# Remove temp documentation files  
rm -f DEPLOYMENT_TROUBLESHOOTING.md DEPLOYMENT_CHECKLIST.md
rm -f PHASE_1_PLANNING.md PHASE_2_IMPLEMENTATION.md PHASE_2_COMPLETE.md
rm -f PROJECT_DOCUMENTATION.txt compile_errors.txt
echo "✅ Removed temporary documentation"

echo ""
echo "🗂️  Essential files that will be pushed:"
echo ""
echo "Backend:"
ls -la backend/src/main/java/com/recommender/**/*.java 2>/dev/null | head -10 || echo "  ✓ All Java files"
echo "  ✓ backend/pom.xml"
echo "  ✓ backend/Dockerfile"
echo ""
echo "Frontend:"
echo "  ✓ frontend/src/**/*.jsx"
echo "  ✓ frontend/src/**/*.js"
echo "  ✓ frontend/src/**/*.css"
echo "  ✓ frontend/package.json"
echo ""
echo "Configuration:"
echo "  ✓ render.yaml"
echo "  ✓ database/schema.sql"
echo "  ✓ README.md"
echo "  ✓ .gitignore"
echo ""

echo ""
echo "📝 Committing..."
git add -A
git commit -m "Phase 2 Complete: Add seeder, pagination, dashboard, optimistic updates, improve Dockerfile" || {
    echo "⚠️  Nothing new to commit"
}

echo ""
echo "🚀 Pushing to GitHub..."
git push origin main

echo ""
echo "✅ PUSHED SUCCESSFULLY!"
echo ""
echo "Next: Render will auto-deploy in 3-5 minutes"
echo "Check: https://dashboard.render.com → course-recommender-api"
echo ""
