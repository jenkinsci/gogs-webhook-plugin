import jenkins.model.*
import hudson.model.*
import hudson.slaves.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.common.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.impl.*
//import hudson.plugins.sshslaves.*

domain = Domain.global()
store = Jenkins.instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()


usernameAndPassword = new UsernamePasswordCredentialsImpl(
        CredentialsScope.GLOBAL,
        "GOGS-USER", "Default GOGS user credentials",
        "butler",
        "butler")

store.addCredentials(domain, usernameAndPassword)
