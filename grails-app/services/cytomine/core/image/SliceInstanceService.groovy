package cytomine.core.image

import cytomine.core.command.AddCommand
import cytomine.core.command.Command
import cytomine.core.command.DeleteCommand
import cytomine.core.command.EditCommand
import cytomine.core.command.Transaction
import cytomine.core.ontology.AnnotationIndex
import cytomine.core.ontology.AnnotationTrack
import cytomine.core.project.Project
import cytomine.core.security.SecUser
import cytomine.core.utils.ModelService
import cytomine.core.utils.SQLUtils
import cytomine.core.utils.Task
import grails.transaction.Transactional
import groovy.sql.Sql
import org.hibernate.FetchMode

import java.nio.file.Paths

import static org.springframework.security.acls.domain.BasePermission.READ
import static org.springframework.security.acls.domain.BasePermission.WRITE
@Transactional
class SliceInstanceService extends ModelService {


    def cytomineService
    def securityACLService
    def dataSource

    def currentDomain() {
        return SliceInstance
    }

    def read(def id) {
        SliceInstance slice = SliceInstance.read(id)
        if (slice) {
            securityACLService.check(slice.container(), READ)
        }
        slice
    }

    def read(ImageInstance image, double c, double z, double t) {
        SliceInstance slice = SliceInstance.createCriteria().get {
            createAlias("baseSlice", "as")
            eq("image", image)
            eq("as.channel", c)
            eq("as.zStack", z)
            eq("as.time", t)
        }
        if (slice) {
            securityACLService.check(slice.container(), READ)
        }
        slice
    }

    def list(ImageInstance image) {
        securityACLService.check(image, READ)
        SliceInstance.createCriteria().list {
            createAlias("baseSlice", "as")
            eq("image", image)
            order("as.time", "asc")
            order("as.zStack", "asc")
            order("as.channel", "asc")
            fetchMode("baseSlice", FetchMode.JOIN)
        }
    }

    def add(def json) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkUser(currentUser)
        securityACLService.check(json.project, Project,READ)
        securityACLService.checkisNotReadOnly(json.project,Project)

        Command c = new AddCommand(user: currentUser)
        executeCommand(c, null, json)
    }

    def update(SliceInstance slice, def json) {
        securityACLService.check(slice.container(),READ)
        securityACLService.check(json.project,Project,READ)
//        securityACLService.checkFullOrRestrictedForOwner(slice.container(),slice.user)
        securityACLService.checkisNotReadOnly(slice.container())
        securityACLService.checkisNotReadOnly(json.project,Project)
        SecUser currentUser = cytomineService.getCurrentUser()

        Command c = new EditCommand(user: currentUser)
        executeCommand(c, slice, json)
    }

    def delete(SliceInstance slice, Transaction transaction = null, Task task = null, boolean printMessage = true) {
//        securityACLService.checkAtLeastOne(slice, READ)
        //TODO security
        SecUser currentUser = cytomineService.getCurrentUser()
        Command c = new DeleteCommand(user: currentUser, transaction: transaction)
        executeCommand(c, slice, null)
    }

    def annotationTrackService
    def deleteDependentAnnotationTrack(SliceInstance slice, Transaction transaction, Task task = null) {
        AnnotationTrack.findAllBySlice(slice).each {
            annotationTrackService.delete(it, transaction, task)
        }
    }

    def annotationIndexService
    def deleteDependentAnnotationIndex(SliceInstance slice, Transaction transaction, Task task = null) {
        AnnotationIndex.findAllBySlice(slice).each {
            it.delete()
        }
    }

    def deleteDependentSampleHistogram(SliceInstance slice, Transaction transaction, Task task = null) {
        SampleHistogram.findAllBySlice(slice).each {
            it.delete()
        }
    }

    def getStringParamsI18n(Object domain) {
        return [domain.id, domain.baseSlice.channel, domain.baseSlice.zStack, domain.baseSlice.time]
    }
}
