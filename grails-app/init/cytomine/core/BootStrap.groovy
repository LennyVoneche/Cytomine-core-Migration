/*
* Copyright (c) 2009-2019. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package cytomine.core

import cytomine.core.utils.CytomineMailService
import cytomine.core.image.multidim.ImageGroupHDF5Service
import cytomine.core.middleware.ImageServerService
import cytomine.core.processing.ImageRetrievalService
import cytomine.core.security.SecUser
import cytomine.core.test.Infos
import cytomine.core.utils.Version
import grails.plugin.springsecurity.SecurityFilterPosition
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.util.Environment
import grails.util.Holders
import groovy.sql.Sql
//import org.grails.commons.ApplicationAttributes
//import org.grails.plugin.resource.ResourceMeta
//import org.grails.plugin.resource.ResourceProcessor
//import org.grails.plugin.resource.URLUtils

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.lang.management.ManagementFactory

/**
 * Bootstrap contains code that must be execute during application (re)start
 */
class BootStrap {

    def grailsApplication

//    def sequenceService
    def marshallersService
    def indexService
    def triggerService
//    def grantService
    def termService
    def tableService
    def secUserService
    def noSQLCollectionService
//
    def retrieveErrorsService
    def bootstrapDataService
//
    def bootstrapUtilsService
    def bootstrapOldVersionService

    def dataSource
    def sessionFactory



    def init = { servletContext ->
//        //Register API Authentifier
        log.info 'Bootstrap.init'
//        SpringSecurityUtils.clientRegisterFilter( 'apiAuthentificationFilter', SecurityFilterPosition.DIGEST_AUTH_FILTER.order + 1)

        log.info "#############################################################################"
        log.info "#############################################################################"
        log.info "#############################################################################"
        String cytomineWelcomMessage = """
                   _____      _                  _
                  / ____|    | |                (_)
                 | |    _   _| |_ ___  _ __ ___  _ _ __   ___
                 | |   | | | | __/ _ \\| '_ ` _ \\| | '_ \\ / _ \\
                 | |___| |_| | || (_) | | | | | | | | | |  __/
                  \\_____\\__, |\\__\\___/|_| |_| |_|_|_| |_|\\___|
                 |  _ \\  __/ |     | |     | |
                 | |_) ||___/  ___ | |_ ___| |_ _ __ __ _ _ __
                 |  _ < / _ \\ / _ \\| __/ __| __| '__/ _` | '_ \\
                 | |_) | (_) | (_) | |_\\__ \\ |_| | | (_| | |_) |
                 |____/ \\___/ \\___/ \\__|___/\\__|_|  \\__,_| .__/
                                                         | |
                                                         |_|
        """
        log.info cytomineWelcomMessage
        log.info "#############################################################################"
        log.info "#############################################################################"
        log.info "#############################################################################"

        [
                "Environment" : Environment.getCurrent().name,
                "Client": Holders.config.grails.client,
                "Server URL": Holders.config.grails.serverURL,
                "Current directory": new File( './' ).absolutePath,
                "HeadLess: ": java.awt.GraphicsEnvironment.isHeadless(),
                "SQL": [url:Holders.config.dataSource.url, user:Holders.config.dataSource.username, password:Holders.config.dataSource.password, driver:Holders.config.dataSource.driverClassName],
                "NOSQL": [host:Holders.config.grails.mongo.host, port:Holders.config.grails.mongo.port, databaseName:Holders.config.grails.mongo.databaseName],
//                  TODO: (Migration)
//                "Datasource properties": servletContext.getAttribute(ApplicationAttributes.APPLICATION_CONTEXT).dataSourceUnproxied.properties,
                "JVM Args" : ManagementFactory.getRuntimeMXBean().getInputArguments()
        ].each {
            log.info "##### " + it.key.toString() + " = " + it.value.toString()
        }
        println getClass().toString() + '002'

        log.info "#############################################################################"
        log.info "#############################################################################"
        log.info "#############################################################################"
        println getClass().toString() + '003 ' + Version.count()
        println getClass().toString() + '003.1 ' + grailsApplication.config.info.app.versionDate as String
        println getClass().toString() + '003.2 ' + grailsApplication.config.info.app.version as String
        if(Version.count()==0) {
            log.info "Version was not set, set to last version"
//            TODO: (Migration)
//            Version.setCurrentVersion(Long.parseLong(grailsApplication.config.info.app.versionDate as String),grailsApplication.config.info.app.versionDate as String)
            Version.setCurrentVersion(Long.parseLong(grailsApplication.config.info.app.versionDate as String),grailsApplication.config.info.app.version as String)
        }

        // TODO : delete this sql in v2.1
        if (!bootstrapUtilsService.checkSqlColumnExistence("sec_user", "origin")) {
            new Sql(dataSource).executeUpdate("ALTER TABLE sec_user ADD COLUMN origin VARCHAR;")
        }

        if (!bootstrapUtilsService.checkSqlColumnExistence("sec_user", "language")) {
            new Sql(dataSource).executeUpdate("ALTER TABLE sec_user ADD COLUMN language VARCHAR;")
        }

        //Initialize marshallers and services
        log.info "init marshaller..."
          marshallersService.initMarshallers()

//        TODO: (Migration) Deprecated
//        log.info "init sequences..."
//        sequenceService.initSequences()

        log.info "init trigger..."
        triggerService.initTrigger()

        log.info "init index..."
        indexService.initIndex()

//        TODO: (Migration) Deprecated
//        log.info "init grant..."
//        grantService.initGrant()

        log.info "init table..."
        tableService.initTable()

        log.info "init term service..."
        termService.initialize() //term service needs userservice and userservice needs termservice => init manualy at bootstrap

        log.info "init retrieve errors hack..."
        retrieveErrorsService.initMethods()

        // Initialize RabbitMQ server
        bootstrapUtilsService.initRabbitMq()
        println "SecUser.count() = " + SecUser.count()
        /* Fill data just in test environment*/
        log.info "fill with data..."
        if (Environment.getCurrent() == Environment.TEST) {
            bootstrapDataService.initData()
            noSQLCollectionService.cleanActivityDB()
            def usersSamples = [
                    [username : Infos.ANOTHERLOGIN, firstname : 'Just another', lastname : 'User', email : grailsApplication.config.grails.admin.email, group : [[name : "Cytomine"]], password : grailsApplication.config.grails.adminPassword, color : "#FF0000", roles : ["ROLE_USER", "ROLE_ADMIN","ROLE_SUPER_ADMIN"]]
            ]
            bootstrapUtilsService.createUsers(usersSamples)

//            mockServicesForTests()

        }  else if (SecUser.count() == 0) {
            //if database is empty, put minimal data
            bootstrapDataService.initData()
        }

        //set public/private keys for special image server user
        //keys regenerated at each deployment with Docker
        //if keys deleted from external config files for security, keep old keys

        if(grailsApplication.config.grails.ImageServerPrivateKey && grailsApplication.config.grails.ImageServerPublicKey) {
            SecUser imageServerUser = SecUser.findByUsername("ImageServer1")
            imageServerUser.setPrivateKey(grailsApplication.config.grails.ImageServerPrivateKey as String)
            imageServerUser.setPublicKey(grailsApplication.config.grails.ImageServerPublicKey as String)
            imageServerUser.save(flush : true)
        }
        if(grailsApplication.config.grails.rabbitMQPrivateKey && grailsApplication.config.grails.rabbitMQPublicKey) {
            SecUser rabbitMQUser = SecUser.findByUsername("rabbitmq")
            if(rabbitMQUser) {
                rabbitMQUser.setPrivateKey(grailsApplication.config.grails.rabbitMQPrivateKey as String)
                rabbitMQUser.setPublicKey(grailsApplication.config.grails.rabbitMQPublicKey as String)
                rabbitMQUser.save(flush : true)
            }
        }

        log.info "init change for old version..."
        // TODO : delete this sql in v2.1
        def exists = new Sql(dataSource).rows("SELECT column_name " +
                "FROM information_schema.columns " +
                "WHERE table_name='version' and column_name='major';").size() == 1;
        if (!exists) {
            new Sql(dataSource).executeUpdate("ALTER TABLE version ADD COLUMN major integer;")
            new Sql(dataSource).executeUpdate("ALTER TABLE version ADD COLUMN minor integer;")
            new Sql(dataSource).executeUpdate("ALTER TABLE version ADD COLUMN patch integer;")
        }

//        TODO: (Migration) Code de migration de version
        bootstrapOldVersionService.execChangeForOldVersion()

        log.info "create multiple IS and Retrieval..."
        bootstrapUtilsService.createMultipleIS()
        bootstrapUtilsService.createMultipleRetrieval()
        bootstrapUtilsService.updateDefaultProcessingServer()


        bootstrapUtilsService.fillProjectConnections();
        bootstrapUtilsService.fillImageConsultations();

        bootstrapUtilsService.initProcessingServerQueues()

//        fixPlugins()
    }

//    private void mockServicesForTests(){
//        //mock services which use IMS
//        ImageGroupHDF5Service.metaClass.callIMSConversion = {
//            SecUser currentUser, def imagesFilenames, String filename -> println "\n\n mocked callIMSConversion \n\n";
//        }
//        ImageServerService.metaClass.getStorageSpaces = {
//            return [[used : 0, available : 10]]
//        }
//        //mock services which use Retrieval
//        ImageRetrievalService.metaClass.doRetrievalIndex = {
//            String url, String username, String password, def image,String id, String storage, Map<String,String> properties -> println "\n\n mocked doRetrievalIndex \n\n";
//                return [code:200,response:"test"]
//        }
//        //mock mail service
//        CytomineMailService.metaClass.send = {
//            String from, String[] to, String cc, String subject, String message, def attachment -> println "\n\n mocked mail send \n\n";
//        }
//    }

//    private void fixPlugins(){
//        //grails resources
//        //for https
//        ResourceProcessor.metaClass.redirectToActualUrl = {
//            ResourceMeta res, HttpServletRequest request, HttpServletResponse response ->
//                String url
//                if (URLUtils.isExternalURL(res.linkUrl)) {
//                    url = res.linkUrl
//
//                } else {
//                    url = grailsApplication.config.grails.serverURL + request.contextPath + staticUrlPrefix + res.linkUrl
//                }
//
//                log.debug "Redirecting ad-hoc resource ${request.requestURI} " +
//                        "to $url which makes it UNCACHEABLE - declare this resource " +
//                        "and use resourceLink/module tags to avoid redirects and enable client-side caching"
//
//                response.sendRedirect url
//        }
//    }
}
