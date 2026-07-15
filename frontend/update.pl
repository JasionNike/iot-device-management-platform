use strict;
use warnings;

open(my $fh, '<:encoding(UTF-8)', 'index.html') or die "Cannot open: $!";
my @lines = <$fh>;
close($fh);

my $changed = 0;

# Line numbers are 1-based in the file
# After alert pagination was added (+1 line), loadOta is now at line 363
# Let's find it dynamically
for (my $i = 0; $i < @lines; $i++) {
    # Replace loadOta method
    if ($lines[$i] =~ /^\s+loadOta:function\(\)\{var self=this;API\.get\('\/ota\/firmwares'\)/) {
        $lines[$i] = "  loadOta:function(){this.loadOtaFirmware();this.loadOtaTasks()},\n";
        $lines[$i] .= "  loadOtaFirmware:function(){var self=this;var p={pageNum:this.otaFirmwarePage.pageNum,pageSize:this.otaFirmwarePage.pageSize};API.get('/ota/firmwares',{params:p}).then(function(r){var d=r.data.data||{};self.otaFirmware=d.list||[];self.otaFirmwarePage.total=d.total||0}).catch(function(){self.otaFirmware=[]})},\n";
        $lines[$i] .= "  loadOtaTasks:function(){var self=this;var p={pageNum:this.otaTaskPage.pageNum,pageSize:this.otaTaskPage.pageSize};API.get('/ota/tasks',{params:p}).then(function(r){var d=r.data.data||{};self.otaTasks=d.list||[];self.otaTaskPage.total=d.total||0;var up=0,total=0;(d.list||[]).forEach(function(t){up+=(t.successCount||0);total+=(t.totalCount||0)});self.otaStats={upgraded:up,pending:total-up}}).catch(function(){self.otaTasks=[]})},\n";
        $changed++;
        print "Replaced loadOta at line ", $i+1, "\n";
        last;
    }
}

# Replace loadProducts method
for (my $i = 0; $i < @lines; $i++) {
    if ($lines[$i] =~ /^\s+loadProducts:function\(\)\{var self=this;API\.get\('\/product\/list'\)/) {
        $lines[$i] = "  loadProducts:function(){var self=this;var p={pageNum:this.productPage.pageNum,pageSize:this.productPage.pageSize};API.get('/product/list',{params:p}).then(function(r){var d=r.data.data||{};var list=d.list||[];self.productList=list;self.productPage.total=d.total||0;self.products=list.map(function(p){return p.productKey})}).catch(function(){self.productList=[]})},\n";
        $changed++;
        print "Replaced loadProducts at line ", $i+1, "\n";
        last;
    }
}

# Add firmware pagination after firmware table
for (my $i = 0; $i < @lines; $i++) {
    if ($lines[$i] =~ /^\s+<el-table :data="otaFirmware"/ && $lines[$i] =~ /创建任务/) {
        $lines[$i] .= "        <el-pagination style=\"margin-top:12px;text-align:right\" :current-page.sync=\"otaFirmwarePage.pageNum\" :page-size=\"otaFirmwarePage.pageSize\" :total=\"otaFirmwarePage.total\" \@current-change=\"loadOtaFirmware\" layout=\"total,prev,pager,next\" small></el-pagination>\n";
        $changed++;
        print "Added firmware pagination after line ", $i+1, "\n";
        last;
    }
}

# Add task pagination after task table
for (my $i = 0; $i < @lines; $i++) {
    if ($lines[$i] =~ /^\s+<el-table :data="otaTasks"/ && $lines[$i] =~ /推进/) {
        $lines[$i] .= "        <el-pagination style=\"margin-top:12px;text-align:right\" :current-page.sync=\"otaTaskPage.pageNum\" :page-size=\"otaTaskPage.pageSize\" :total=\"otaTaskPage.total\" \@current-change=\"loadOtaTasks\" layout=\"total,prev,pager,next\" small></el-pagination>\n";
        $changed++;
        print "Added task pagination after line ", $i+1, "\n";
        last;
    }
}

# Add product pagination after product table
for (my $i = 0; $i < @lines; $i++) {
    if ($lines[$i] =~ /^\s+<el-table :data="productList"/ && $lines[$i] =~ /删除/) {
        $lines[$i] .= "      <el-pagination style=\"margin-top:16px;text-align:right\" :current-page.sync=\"productPage.pageNum\" :page-size=\"productPage.pageSize\" :total=\"productPage.total\" \@current-change=\"loadProducts\" layout=\"total,prev,pager,next\" small></el-pagination>\n";
        $changed++;
        print "Added product pagination after line ", $i+1, "\n";
        last;
    }
}

open(my $out, '>:encoding(UTF-8)', 'index.html') or die "Cannot write: $!";
print $out @lines;
close($out);

print "$changed changes applied\n";
