# GogsWebhook Smoke test

* [x] Move sample project to “/target” work directory
* [x] Initialise local sample project as git directory
* [x] Configure the username and mail address (on project level)
* [x] Stage local file to local repository
* [x] Commit files to local repository
* [x] Wait for GOGS server to be available
* [x] Create GOGS project under user butler
* [x] Configure GOGS as remote
* [x] Push master branch to GOGS
* [ ] Create JENKINS project pointing to GOGS project 
* [ ] (validation)Force build on JENKINS
* [ ] (validation) Check JENKINS build result
* [x] Create GOGS web hook
* [ ] Change file in local repo
* [ ] Add and commit changed file in local repository
* [ ] Push update to GOGS repository
* [ ] Wait for a certain time that a JENKINS build was started 
* [ ] Check that the JENKINS successfully completed with the correct changes