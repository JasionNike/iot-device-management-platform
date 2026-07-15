use strict;
use warnings;

open(my $fh, '<:encoding(UTF-8)', 'index.html') or die "Cannot open: $!";
my @lines = <$fh>;
close($fh);

my $changed = 0;

for (my $i = 0; $i < @lines; $i++) {
    my $line = $lines[$i];

    # Fix doLogin method: add captcha check and captchaKey before API call
    if ($line =~ /doLogin:function/) {
        # Add captcha validation before loginLoading=true
        $line =~ s/(self\.loginLoading=true;)/if(!self.loginForm.captchaCode){self.\$message.error('请输入验证码');return} self.loginForm.captchaKey=self.captchaKey; $1/;
        # Add loadCaptcha after successful login (before self.$nextTick)
        $line =~ s/(self\.loginLoading=false;self\.\$nextTick)/self.loginLoading=false;self.loadCaptcha();self.\$nextTick/;
        # Add loadCaptcha after failed login
        $line =~ s/(self\.loginLoading=false;var msg=)/self.loginLoading=false;self.loadCaptcha();var msg=/;
        $lines[$i] = $line;
        $changed++;
        print "Updated doLogin at line ", $i+1, "\n";
    }

    # Fix handleUserCmd: clear form fields and load captcha on logout
    if ($line =~ /handleUserCmd:function/) {
        $line =~ s/username:'admin',password:'admin123'/username:'',password:'',captchaKey:'',captchaCode:''/;
        $line =~ s/\}\},\}$/};this.loadCaptcha()}},/;
        $lines[$i] = $line;
        $changed++;
        print "Updated handleUserCmd at line ", $i+1, "\n";
    }

    # Add loadCaptcha method before handleUserCmd
    if ($line =~ /handleUserCmd:function/ && $i > 0 && $lines[$i-1] =~ /^\s+\},$/) {
        $lines[$i-1] = "  loadCaptcha:function(){var self=this;API.get('/auth/captcha').then(function(r){var d=r.data.data||{};self.captchaKey=d.captchaKey;self.captchaImage=d.captchaImage;self.loginForm.captchaCode=''}).catch(function(){self.captchaImage='';self.\$message.error('验证码加载失败')})},\n" . $lines[$i-1];
        $changed++;
        print "Added loadCaptcha method\n";
    }
}

# Add mounted hook to load captcha on page load
for (my $i = 0; $i < @lines; $i++) {
    if ($lines[$i] =~ /mounted:function/) {
        $lines[$i] =~ s/(if\(!this\.loggedIn\)return;)/$1var self=this;self.loadCaptcha();/;
        $changed++;
        print "Added captcha load in mounted\n";
        last;
    }
}

open(my $out, '>:encoding(UTF-8)', 'index.html') or die "Cannot write: $!";
print $out @lines;
close($out);

print "$changed changes applied\n";
