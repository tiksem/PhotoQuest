module.exports = function (grunt) {

    require('load-grunt-tasks')(grunt);

    // 1. All configuration goes here
    var i = 0;
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
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/ng-tags-input.js",
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
                    "../../PhotoQuestOut/grunt/ngAnnotate/web/WEB-INF/resources/js/header.js",
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
                    '../../PhotoQuestOut/grunt/templates.js'
                ],
                dest: '../../PhotoQuestOut/grunt/ngAnnotate/one.js'
            },
            css: {
                src: ['../web/WEB-INF/resources/css/*.css'],
                dest: '../../PhotoQuestOut/grunt/one.css'
            }
        },

        'closure-compiler': {
            frontend: {
                js: '../../PhotoQuestOut/grunt/ngAnnotate/one.js',
                jsOutputFile: '../../PhotoQuestOut/grunt/resources/minified.js',
                maxBuffer: 500,
                options: {
                    compilation_level: 'SIMPLE_OPTIMIZATIONS',
                    language_in: 'ECMASCRIPT5'
                }
            }
        },

        html2js: {
            options: {
                rename: function (moduleName) {
                    var first = moduleName.lastIndexOf("html/");
                    return moduleName.substring(first, moduleName.length);
                }
            },
            main: {
                src: ["../../PhotoQuestOut/grunt/html/web/WEB-INF/resources/html/*.html"],
                //src: ["../web/WEB-INF/resources/html/*.html"],
                dest: '../../PhotoQuestOut/grunt/templates.js'
            }
        },

        cssmin: {
            options: {
                shorthandCompacting: false,
                roundingPrecision: -1
            },
            target: {
                files: {
                    '../../PhotoQuestOut/grunt/resources/minified.css':
                        ['../../PhotoQuestOut/grunt/one.css']
                }
            }
        },

        template: {
            target: {

            }
        },

        minifyHtml: {
            options: {
                empty: true
            },
            dev: {
                files: [
                    {
                        src: ['../web/WEB-INF/resources/one.html'], // Actual pattern(s) to match.
                        dest: "../../PhotoQuestOut/grunt/resources/index.html"
                    }
                ]
            },
            dev2: {
                files: [
                    {
                        expand: true,
                        src: ['../web/WEB-INF/resources/html/*.html'], // Actual pattern(s) to match.
                        dest: "../../PhotoQuestOut/grunt/html/sdfdssdf"
                    }
                ]
            }
        },

        copy: {
            images: {
                files: [
                    // makes all src relative to cwd
                    {
                        expand: true,
                        cwd: '../web/WEB-INF/resources/images/',
                        src: ['**'],
                        dest: '../../../../PhotoQuestOut/grunt/resources/images/'
                    }
                ]
            }
        }
    });

    // 3. Where we tell Grunt we plan to use this plug-in.
    grunt.loadNpmTasks('grunt-contrib-concat');
    grunt.loadNpmTasks('grunt-ng-annotate');
    grunt.loadNpmTasks('grunt-closure-compiler');
    grunt.loadNpmTasks('grunt-html2js');
    grunt.loadNpmTasks('grunt-contrib-cssmin');
    grunt.loadNpmTasks('grunt-minify-html');
    grunt.loadNpmTasks('grunt-contrib-copy');

    grunt.registerMultiTask('template', 'Templates module replacing...', function() {
        var options = {
            encoding: 'UTF-8'
        };
        var file = grunt.file.read('../../PhotoQuestOut/grunt/ngAnnotate/one.js', options);
        file = file.replace('/*HTML_TEMPLATES_PLACEHOLDER*/', ", 'templates-main'");
        grunt.file.write('../../PhotoQuestOut/grunt/ngAnnotate/one.js', file, options);
    });

    // 4. Where we tell Grunt what to do when we type "grunt" into the terminal.
    grunt.registerTask('default', ['ngAnnotate', 'minifyHtml', 'html2js', 'concat',
        'template', 'cssmin', 'closure-compiler', 'copy']);

};