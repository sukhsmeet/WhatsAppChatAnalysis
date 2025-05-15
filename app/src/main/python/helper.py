from urlextract import URLExtract
from wordcloud import WordCloud
import pandas as pd
from collections import Counter
import emoji
import matplotlib.pyplot as plt
import io
import base64

extracter = URLExtract()

def users(data):
    return data['Sender'].unique().tolist()

def fetch_stats(selected_user,df):
    
    if selected_user != "Overall":
        df = df[df['Sender'] == selected_user]
        
    # number of msgs
    num_msgs = df.shape[0]
        # number of words
    words = []
    for message in df['Message']:
        words.extend(message.split())
        
    num_media_msgs = df[df['Message']=="<Media omitted>"].shape[0]  
    
    links = []
    for message in df['Message']:
        links.extend(extracter.find_urls(message)) 
       
    return num_msgs,len(words),num_media_msgs,len(links)   



def most_busy_user(df):
    x = df['Sender'].value_counts().head()
    percent_df = round((df['Sender'].value_counts()/df.shape[0])*100,2).reset_index().rename(columns={'count':'Percent'})
    plt.figure(figsize=(10, 8))
    plt.bar(x.index, x.values, color='skyblue')
    plt.title("Most Busy Users")
    plt.xlabel('Labels')
    plt.ylabel('Values')
    plt.tight_layout()

    buf = io.BytesIO()
    plt.savefig(buf, format='png')
    plt.close()
    buf.seek(0)

    img_bytes = buf.read()
    encoded = base64.b64encode(img_bytes).decode('utf-8')
    return encoded
    #return x,percent_df



def create_wordcloud(selected_user,df):
    if selected_user != "Overall":
        df = df[df['Sender'] == selected_user]
        
         
    temp =df[df['Message'] != '<Media omitted>']
    f = open('D:\VS Code\ML\Whatsapp Chat Analysis\stop_hinglish.txt','r')
    stop_words = f.read()
    
    def remove_stopwords(message):
        y=[]
        for word in message:
            if word not in stop_words:
                y.append(word)
        return " ".join(y)            
        
    wc = WordCloud(width=500,height=500,min_font_size=10,background_color='white')
    temp['Message'] = temp['Message'].apply(remove_stopwords)
    df_wc = wc.generate(temp['Message'].str.cat(sep = " "))   
    return df_wc

def most_common_words(selected_user,df):
    if selected_user != "Overall":
        df = df[df['user'] == selected_user]
        
    temp =df[df['Message'] != '<Media omitted>']
    f = open('D:\VS Code\ML\Whatsapp Chat Analysis\stop_hinglish.txt','r')
    stop_words = f.read()
    words_without_stopwords = []

    for message in temp['Message']:
        for word in message.lower().split():
            if word not in stop_words:
                words_without_stopwords.append(word)    
       
    df_mostwords  = pd.DataFrame(Counter(words_without_stopwords).most_common(20))
    return df_mostwords    

def emoji_helper(selected_user,df):
    if selected_user != "Overall":
        df = df[df['user'] == selected_user]
        
    emojis = []
    for message in df['Message']:
        emojis.extend([c for c in message if emoji.is_emoji(c)])    
        
    emoji_df = pd.DataFrame(Counter(emojis).most_common(len(emojis)))
    return emoji_df   


def monthly_timeline(selected_user,df):
    if selected_user != "Overall":
        df = df[df['user'] == selected_user]
        
    timeline  = df.groupby(['Year','Month']).count()['Message'].reset_index()
    
    time = []
    for i in range(timeline.shape[0]):
        time.append(timeline['Month'][i] + "-"+str(timeline['Year'][i]))    
        
    timeline['Time'] = time
    return timeline


def daily_timeline(selected_user,df):
    if selected_user != "Overall":
        df = df[df['user'] == selected_user]
        
    daily_timeline  = df.groupby(['Date']).count()['Message'].reset_index()   
    
    return daily_timeline


def week_activity(selected_user,df):
    if selected_user != "Overall":
        df = df[df['user'] == selected_user]
        
    return df['Day_name'].value_counts()    

def month_activity(selected_user,df):
    if selected_user != "Overall":
        df = df[df['user'] == selected_user]
            
    return df['Month'].value_counts() 

def activity_heatmap(selected_user):
    if selected_user != "Overall":
        df = df[df['user'] == selected_user]
        
    pivot_heatmap = df.pivot_table(index='Day_name',columns='Period',values='Message',aggfunc='count').fillna(0)
    return pivot_heatmap
    
       
    
    
        
     
    
    
    
    
        
        
    