module.exports = function (grunt) {

    // 1. All configuration goes here 
    grunt.initConfig({
        pkg: grunt.file.readJSON('package.json'),

        ngAnnotate: {
            options: {
                singleQuotes: true
            },
            app1: {
                files: [
                    {
                        expand: true,
                        src: ['../web/WEB-INF/resources/js/*.js'],
                        dest: '../../PhotoQuestOut/grunt/ngAnnotate/sdfsadfsadf'
                    }
                ]
            }
        },

        concat: {
            app1: {
                src: [
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/angular-file-upload-shim.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/angular.min.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/jquery-2.1.1.min.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/utilities.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/ngDialog.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/string.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/angular-file-upload.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/angular-cookies.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/array.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/dirPagination.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/object.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/pagination-init.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/infinite_scroll.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/http.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/jquery.autocomplete.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/scroll-utils.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/ng-tags-input/ng-tags-input.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/controllers.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/angular-sanitize.min.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/translation.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/xregexp-all-min.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/main.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/thumbnail.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/file-select-container.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/tab-link.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/location_input.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/country_input.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/city_input.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/search_select.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/shortcut.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/profile_buttons.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/login.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/register.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/photoquests.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/photos.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/people.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/photo.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/profile.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/dialogs.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/messages.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/replies.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/feed.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/settings.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/first_quest.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/progress.js",
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/about.js",
                ],
                dest: '../../PhotoQuestOut/grunt/ngAnnotate/one.js'
            }
        },

        'closure-compiler': {
            frontend: {
                js: '../../PhotoQuestOut/grunt/ngAnnotate/one.js',
                jsOutputFile: '../../PhotoQuestOut/grunt/minified.js',
                maxBuffer: 500,
                options: {
                    compilation_level: 'SIMPLE_OPTIMIZATIONS',
                    language_in: 'ECMASCRIPT5'
                }
            }
        }
    });

    // 3. Where we tell Grunt we plan to use this plug-in.
    grunt.loadNpmTasks('grunt-contrib-concat');
    grunt.loadNpmTasks('grunt-ng-annotate');
    grunt.loadNpmTasks('grunt-closure-compiler');

    // 4. Where we tell Grunt what to do when we type "grunt" into the terminal.
    grunt.registerTask('default', ['ngAnnotate', 'concat', 'closure-compiler']);

};