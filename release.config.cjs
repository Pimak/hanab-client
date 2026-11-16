module.exports = {
  branches: ['main'],
  tagFormat: 'v${version}',
  plugins: [
    '@semantic-release/commit-analyzer',
    '@semantic-release/release-notes-generator',
    '@semantic-release/changelog',
    [
      '@semantic-release/exec',
      {
        prepareCmd:
          './mvnw --batch-mode --no-transfer-progress versions:set -DnewVersion=${nextRelease.version} -DgenerateBackupPoms=false',
        publishCmd:
          './mvnw --batch-mode --no-transfer-progress deploy -Prelease'
      }
    ],
    [
      '@semantic-release/git',
      {
        assets: ['CHANGELOG.md', 'pom.xml'],
        message: 'chore(release): ${nextRelease.version} [skip ci]'
      }
    ],
    '@semantic-release/github'
  ]
};
