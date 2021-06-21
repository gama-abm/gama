#!/bin/bash




function update_tag() {
	echo "update tag " $1 
	git config --global user.email "my.gama.bot@gmail.com"
	git config --global user.name "GAMA Bot"
	git remote rm origin
	git remote add origin https://gama-bot:$BOT_TOKEN@github.com/gama-platform/gama.git
	git config remote.origin.fetch "+refs/heads/*:refs/remotes/origin/*"
	git fetch origin
	git checkout --track origin/master
	git pull
	git status
	git push origin :refs/tags/$1
	git tag -d $1
	git tag -fa $1 -m "$1"
	git push --tags -f
	git ls-remote --tags origin
	git show-ref --tags
}


set -e
echo "github_release_continuous_withjdk"		
COMMIT=$@

REPO="gama-platform/gama"
RELEASE="1.8.2"
thePATH="$GITHUB_WORKSPACE/ummisco.gama.product/target/products/Gama1.7"















COMMIT="${COMMIT:0:7}"
BRANCH_NAME=$(echo $GITHUB_REF | cut -d'/' -f 3)
COMMIT=$(echo $GITHUB_SHA | cut -c1-8)

timestamp=$(date '+_%D')

SUFFIX=$timestamp'_'$COMMIT'.zip'
echo $SUFFIX



n=0
RELEASEFILES[$n]="$thePATH-linux.gtk.x86_64.zip"
NEWFILES[$n]='GAMA_1.8.2_Alpha_Linux'$SUFFIX 
n=1
RELEASEFILES[$n]="$thePATH-macosx.cocoa.x86_64.zip"
NEWFILES[$n]='GAMA_1.8.2_Alpha_Mac'$SUFFIX
n=2
RELEASEFILES[$n]="$thePATH-win32.win32.x86_64.zip" 
NEWFILES[$n]='GAMA_1.8.2_Alpha_Win'$SUFFIX
n=3
RELEASEFILES[$n]="$thePATH-linux.gtk.x86_64_withJDK.zip"
NEWFILES[$n]='GAMA_1.8.2_Alpha_withJDK_Linux'$SUFFIX
n=4
RELEASEFILES[$n]="$thePATH-win32.win32.x86_64_withJDK.zip" 
NEWFILES[$n]='GAMA_1.8.2_Alpha_withJDK_Win'$SUFFIX
n=5
RELEASEFILES[$n]="$thePATH-macosx.cocoa.x86_64_withJDK.zip"
NEWFILES[$n]='GAMA_1.8.2_Alpha_withJDK_Mac'$SUFFIX
 

i=0
for (( i=0; i<6; i++ ))
do
	FILE="${RELEASEFILES[$i]}"
	NFILE="${NEWFILES[$i]}"
	ls -sh $FILE
	echo $NFILE
done





LK1="https://api.github.com/repos/gama-platform/gama/releases/tags/$RELEASE"

echo   "Getting info of release Continuous...  "
RESULT1=`curl  -s -X GET \
-H "Authorization: token $BOT_TOKEN"   \
"$LK1"`	
echo $RESULT1

	json=$RESULT1
	prop='id'
	
    temp=`echo $json | sed 's/\\\\\//\//g' | sed 's/[{}]//g' | awk -v k="text" '{n=split($0,a,","); for (i=1; i<=n; i++) print a[i]}' | sed 's/\"\:\"/\|/g' | sed 's/[\,]/ /g' | sed 's/\"//g' | grep -w $prop`
    
	assets=`echo ${temp##*|}`

	for theid in $assets; do
		if [ "$theid" != "id:" ]; then
	LK1="https://api.github.com/repos/gama-platform/gama/releases/$theid"

	echo   "Deleting release Continuous...  "
	RESULT1=`curl  -s -X DELETE \
	-H "Authorization: token $BOT_TOKEN"   \
	"$LK1"`	
	echo $RESULT1
	break
		fi
	done 


	#update_tag $RELEASE

	echo   "Creating release Continuous...  "
LK="https://api.github.com/repos/gama-platform/gama/releases"

  RESULT=` curl -s -X POST \
  -H "X-Parse-Application-Id: sensitive" \
  -H "X-Parse-REST-API-Key: sensitive" \
  -H "Authorization: token $BOT_TOKEN"   \
  -H "Content-Type: application/json" \
  -d '{"tag_name": "'$RELEASE'", "name":"Alpha Version 1.8.2 ('$COMMIT')","body":"Alpha release for GAMA 1.8.2, which adds compatibility with JDK 11+ . Please test and report issues","draft": false,"prerelease": true}' \
    "$LK"`
echo $RESULT	

















echo
echo "Getting info of $RELEASE tag..."
echo 
LK="https://api.github.com/repos/gama-platform/gama/releases/tags/$RELEASE"

  RESULT=` curl -s -X GET \
  -H "X-Parse-Application-Id: sensitive" \
  -H "X-Parse-REST-API-Key: sensitive" \
  -H "Authorization: token $BOT_TOKEN"   \
  -H "Content-Type: application/json" \
  -d '{"name":"value"}' \
    "$LK"`
echo $RESULT	
RELEASEID=`echo "$RESULT" | sed -ne 's/^  "id": \(.*\),$/\1/p'`
echo $RELEASEID


echo 
echo "Upload new files..."
echo

for (( i=0; i<6; i++ ))
do     
	FILE="${RELEASEFILES[$i]}"
	NFILE="${NEWFILES[$i]}"

  FILENAME=`basename $FILE`
  echo   "Uploading $NFILE...  "
  LK="https://uploads.github.com/repos/gama-platform/gama/releases/$RELEASEID/assets?name=$NFILE"
  
  RESULT=`curl -s -w  "\n%{http_code}\n"                   \
    -H "Authorization: token $BOT_TOKEN"                \
    -H "Accept: application/vnd.github.manifold-preview"  \
    -H "Content-Type: application/zip"                    \
    --data-binary "@$FILE"                                \
    "$LK"`
	echo $RESULT
done 
 
echo DONE
