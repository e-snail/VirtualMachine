.class public Lvm/androidvm/Bean;
.super Ljava/lang/Object;
.source "Bean.java"


# instance fields
.field public value:I


# direct methods
.method static constructor <clinit>()V
    .registers 2

    .prologue
    .line 12
    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;

    const-string v1, "androidvm: Bean static code"

    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V

    .line 13
    return-void
.end method

.method public constructor <init>()V
    .registers 1

    .prologue
    .line 7
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method
