# srnyx uses Google Drive Sync which likes to lock files and directories, preventing the project from being built
# Deleting the build directories fixes this, but since each version has its own build directory, it's a pain to do manually
# Example usages:
#   clean.sh            (cleans all versions with confirmation)
#   clean.sh -y         (cleans all versions without confirmation)
#   clean.sh -v 1.21    (cleans 1.21 with confirmation)
#   clean.sh -v 1.21 -y (cleans 1.21 without confirmation)


cd versions || exit 1

skip_confirm=false

if [[ $1 == "-v" ]]; then
  # Get specified version
  versions=$2
  if [[ $3 == "-y" ]]; then
    skip_confirm=true
  fi
else
  # Get all versions
  versions=$(find . -maxdepth 1 -type d | grep -v '^.$' | sed 's/.\///')
  if [[ $1 == "-y" ]]; then
    skip_confirm=true
  fi
fi

# Ask user for confirmation (unless skipped)
if [[ $skip_confirm == false ]]; then
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
