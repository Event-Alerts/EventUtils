# srnyx uses Google Drive Sync which likes to lock files and directories, preventing the project from being built
# Deleting the build directories fixes this, but since each version has its own build directory, it's a pain to do manually

cd versions || exit 1

# Get list of all versions
versions=$(find . -maxdepth 1 -type d | grep -v '^.$' | sed 's/.\///')

# Ask user for confirmation (unless skipped)
if [[ $1 != "-y" ]]; then
  echo "This will remove all build directories for the following versions:"
  echo "$versions"
  read -p "Are you sure you want to continue? (y/n) " -n 1 -r
  echo
  if [[ ! $REPLY =~ ^[Yy]$ ]]; then
      exit 1
  fi
fi

# Remove each version's build directory
for version in $versions; do
    rm -rf "$version"/build
done
