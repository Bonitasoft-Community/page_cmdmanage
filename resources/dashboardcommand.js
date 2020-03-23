'use strict';
/**
 * 
 */

(function() {

var appCommand = angular.module('bonitacommands', ['ui.bootstrap','angularFileUpload']);


// appCommand.config();

// Constant used to specify resource base path (facilitates integration into a Bonita custom page)
appCommand.constant('RESOURCE_PATH', 'pageResource?page=custompage_cmdmanage&location=');


	   
// User app list controller
appCommand.controller('CommandsController', 
	function ( $scope, $http, $upload) {
	this.list = {items : [], pageIndex : 0, pageSize : 5, totalCount : 0};
	this.messageList='';
	
	this.navbaractiv='CUSTOMCMD';
	
	
	this.cmd = {
            name: 'Command Ping',
            classname:'org.bonitasoft.cmd.CmdPing',
			description: 'Command Ping to just reply when we call it',
			serverjardependenciesfilename:''
		};
	
	this.getNavClass = function( tabtodisplay )
	{
		if (this.navbaractiv === tabtodisplay)
			return 'ng-isolate-scope active';
		return 'ng-isolate-scope';
	}
	

	
	
	this.refresh = function()
	{
		console.log("Refresh");
		var t = new Date();
		var self=this;
		
		self.message='';
		self.errormessage='';
		self.inprogress=true;
		
		var url='?page=custompage_cmdmanage&action=listcmd&t='+t;
		
		$http.get(url)
		.success(function(jsonResult, statusHttp, headers, config) {
	
			// connection is lost ?
			if (statusHttp==401 || typeof jsonResult === 'string') {
				console.log("Redirected to the login page !");
				window.location.reload();
			}
			
				// console.log('getList='+angular.toJson(result));
			self.listcmdsystem = jsonResult.listcmdsystem;
			self.listcmdcustom = jsonResult.listcmdcustom;
			self.messageList='';
			self.inprogress=false;
				})
		.error( function ( jsonResult ) {
			console.log('error');
			self.messageList=jsonResult;
			self.inprogress=false;
				}
				);
	}
	
	this.refresh();
	
	this.undeployCommand = function( cmdid ) {
		var t = new Date();
		var url='?page=custompage_cmdmanage&action=undeploy&cmdid='+cmdid+"&t="+t;
		var self=this;
		
		self.message='';
		self.errormessage='';
		self.inprogress=true;
			
		$http.get(url)
		.success(function(jsonResult, statusHttp, headers, config) {
	
			// connection is lost ?
			if (statusHttp==401 || typeof jsonResult === 'string') {
				console.log("Redirected to the login page !");
				window.location.reload();
			}

			console.log('undeploy '+result);
			self.listcmdsystem = jsonResult.listcmdsystem;
			self.listcmdcustom = jsonResult.listcmdcustom;
			self.messageList='';
			self.inprogress=false;
				})
		.error( function ( jsonResult ) {
			self.messageList=jsonResult;
			self.inprogress=false;
								}
				);
		};
		
		
	
		$scope.message='';
		
		
		
		// ------------------------------------ deploy
		this.deployCommand = function ()
		{
			var d = new Date();
			var self=this;
			
			self.message='';
			self.errormessage='';
			self.inprogress=true;
			var json = encodeURI( angular.toJson( self.cmd, false));
			var url='?page=custompage_cmdmanage&action=deploy&paramjson='+json+"&t="+d.getTime();
			
			$http.get(url)
			.success(function(jsonResult, statusHttp, headers, config) {
		
				// connection is lost ?
				if (statusHttp==401 || typeof jsonResult === 'string') {
					console.log("Redirected to the login page !");
					window.location.reload();
				}

				self.message =jsonResult.message;
				self.errormessage =jsonResult.errormessage;
				self.inprogress=false;
				self.listcmdsystem = jsonResult.listcmdsystem;
				self.listcmdcustom = jsonResult.listcmdcustom;
													
				})
			.error( function ( jsonResult ) {
				self.message ='Error during deployment '+jsonResult;
				self.inprogress=false;
				});

		};
		
		// -------------------------------- upload
		this.onFileSelect = function($files) {
							
			var self=this;
			//$files: an array of files selected, each file has name, size, and type.
			for (var i = 0; i < $files.length; i++) {
				var file = $files[i];
				$scope.upload = $upload.upload({
					url: 'fileUpload', //upload.php script, node.js route, or servlet url
					method: 'POST',
							//headers: {'header-key': 'header-value'},
							//withCredentials: true,
					data: {myObj: $scope.myModelObj},
					file: file, // or list of files ($files) for html5 only
					    //fileName: 'doc.jpg' or ['1.jpg', '2.jpg', ...] // to modify the name of the file(s)
					    // customize file formData name ('Content-Desposition'), server side file variable name. 
					    //fileFormDataName: myFile, //or a list of names for multiple files (html5). Default is 'file' 
					    // customize how data is added to formData. See #40#issuecomment-28612000 for sample code
					    //formDataAppender: function(formData, key, val){}
				}).progress(function(evt) {
					console.log('percent: ' + parseInt(100.0 * evt.loaded / evt.total));
				}).success(function(data, status, headers, config) {
					self.cmd.serverjardependenciesfilename = self.cmd.serverjardependenciesfilename+';'+data;
					console.log("Success, file uploaded="+data+" list="+self.cmd.serverjardependenciesfilename)
				});
				}
			};	
			
			
		// call the command
		this.callcmd = {'id': '', 'parameters':''};
		
		this.callcommand = function()
		{
			this.dothecallcommand(this.callcmd.parameters);
			
		}
		this.callping = function()
		{
			this.dothecallcommand('{"verb":"PING"}');
		}
		this.callhelp = function()
		{
			this.dothecallcommand('{"verb":"HELP"}');
		}
			
		this.dothecallcommand = function( parameters)
		{
			var d = new Date();
			var self=this;
			
			self.callmessage='';
			self.callerrormessage='';
			self.inprogress=true;
			var jsonparam= {'id': self.callcmd.id, 'parameters':parameters};
			var json = encodeURI( angular.toJson( jsonparam, false));
			var url='?page=custompage_cmdmanage&action=callcmd&paramjson='+json+"&t="+d.getTime();

			$http.get(url)
			.success(function(jsonResult, statusHttp, headers, config) {
		
				// connection is lost ?
				if (statusHttp==401 || typeof jsonResult === 'string') {
					console.log("Redirected to the login page !");
					window.location.reload();
				}
				self.callmessage =jsonResult.message;
				self.callerrormessage =jsonResult.errormessage;
				self.inprogress=false;
				})
			.error( function ( jsonResult ) {
				self.callerrormessage ='Error during call '+jsonResult;
				self.inprogress=false;
				});

		}
			
	});
		
	
})();
	
