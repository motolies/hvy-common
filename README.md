# hvy-common

## Register
[문서를 참고 하세요.](https://central.sonatype.org/publish/publish-guide/)

## Deploy
[DEPLOY.md를 참고하세요.](docs/DEPLOY.md)


## tags
```shell
# local tag 삭제
git tag -d {태그명}

# remote tag 삭제
git push --delete origin {태그명}

# local 내 전체 tag 삭제
git tag -d $(git tag -l)

# remote 내 전체 tag 삭제
git push origin --delete $(git tag -l)
```