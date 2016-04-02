'use strict';
/**
 * 
 */

(function() {

var appCommand = angular.module('bonitacommands', ['ui.bootstrap','angularFileUpload']);


// appCommand.config();

// Constant used to specify resource base path (facilitates integration into a Bonita custom page)
appCommand.constant('RESOURCE_PATH', 'pageResource?page=custompage_cmdmanage&location=');

appCommand.controller('DashboardCommandController',
	function () {  });

/* appCommand.directive("listcommands", ['RESOURCE_PATH', function(RESOURCE_PATH) {
	return {
		restrict: 'E',
		templateUrl: RESOURCE_PATH +'directives/listcommands.html'
	};
	**/

var myCommandsList = [];
/*  {
	   cmdName : 'ChangeTimer',
	   cmdDescription : 'Change a timer date',
	   cmdDependency : 'ChangeTimer-1.0.0.jar',
	   cmdJarPath : 'E:/dev/workspace/cmdChangeTimer/target/ChangeTimer-1.0.0.jar',
	   cmdClass : 'com.bonitasoft.command.ChangeTimer',
	   } ];
 */
	   
	   
// User app list controller
appCommand.controller('CommandsController', 
	function ( $scope, $http) {
	this.list = {items : [], pageIndex : 0, pageSize : 5, totalCount : 0};
	this.list.items = myCommandsList;
	this.messageList='';
	
	
	this.withSystem=true;
	
	this.refresh = function()
	{
		var me=this;
		var url='?page=custompage_cmdmanage&action=listcmd&withsystem='+this.withSystem;
		
		$http.get(url)
		.success( function ( result ) {
								console.log('getList '+result);
								me.list.items = result;
								me.messageList='';
				})
		.error( function ( result ) {
								alert('error');
								me.messageList=result;
								}
				);
	}
	
	this.refresh();
	
	this.undeploy = function( cmdid ) {
		var url='?page=custompage_cmdmanage&action=undeploy&cmdid='+cmdid+'&withsystem='+this.withSystem;
		var me=this;
	
		$http.get(url)
		.success( function ( result ) {
								console.log('undeploy '+result);
								me.list.items = result.listcmd;
								alert(result.shortmessage);
				})
		.error( function ( result ) {
								alert('error during undeploy');
								me.messageList=result;
								}
				);
		};
	
	});
	
	
appCommand.controller('DeployController', 
	function($scope, $http, $upload) {
		$('#deploywait').hide();
	
		$scope.cmd = {
            name: 'timer',
            classname:'com.bonitasoft.command.ChangeTimer',
			description: 'Timer value update',
			serverjardependenciesfilename:''
		};
		$scope.message='';
		
		$scope.setMessage = function (message, error)
		{
			$scope.message=message;
		};
		
		// ------------------------------------ deploy
		$scope.deployCommand = function ()
		{
			$('#deploywait').show();
			$('#deploybtn').hide();
			var url='?page=custompage_cmdmanage&action=deploy&name='+$scope.cmd.name;
			url = url + '&classname='+$scope.cmd.name;
			url = url + '&description='+$scope.cmd.description;		
			url = url + '&serverjardependenciesfilename='+$scope.cmd.serverjardependenciesfilename;
			
			$http.get( url )
				.success( function ( jsonResult ) {
								alert(jsonResult.shortmessage);
								$scope.message =jsonResult.message;
								$scope.errormessage =jsonResult.errormessage;
								$('#deploywait').hide();
								$('#deploybtn').show();
								}
						)
				.error( function ( result ) {
								alert('error');
								$scope.setMessage('Error during deployment '+result,1);
								$('#deploywait').hide();
								$('#deploybtn').show();
								
								}
						);


		};
		
		// -------------------------------- upload
		$scope.onFileSelect = function($files) {
								
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
						$scope.cmd.serverjardependenciesfilename = $scope.cmd.serverjardependenciesfilename+';'+data;
					});
				}
			};	
	});
	
	
})();
